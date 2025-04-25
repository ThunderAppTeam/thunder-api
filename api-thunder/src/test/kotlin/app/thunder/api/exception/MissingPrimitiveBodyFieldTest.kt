package app.thunder.api.exception

import app.thunder.api.ThunderApiWebTest
import java.time.LocalDateTime
import org.assertj.core.api.Assertions.assertThat
import org.instancio.Instancio
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class MissingPrimitiveBodyFieldTest : ThunderApiWebTest() {

    @ParameterizedTest
    @ValueSource(strings = ["reviewCompleteNotify", "reviewRequestNotify", "marketingAgreement"])
    @DisplayName("회원 설정 수정 요청에 Boolean 필드가 빠지면 400 MISSING_REQUIRED_PARAMETER로 응답하고 저장된 설정을 바꾸지 않는다")
    fun settingsWithoutBooleanField(missingField: String) {
        // given
        insertMember(SETTINGS_MEMBER_ID)
        jdbcTemplate.update(
            "INSERT INTO member_setting (member_id, settings, created_at) VALUES (?, ?::jsonb, ?)",
            SETTINGS_MEMBER_ID,
            """{"reviewCompleteNotify":true,"reviewRequestNotify":true,"marketingAgreement":true}""",
            LocalDateTime.now(),
        )
        val body = SETTINGS_FIELDS.filter { it != missingField }.joinToString(",", "{", "}") { "\"$it\":false" }
        val request = put("/v1/member/settings")
            .header(AUTHORIZATION, bearerToken(SETTINGS_MEMBER_ID))
            .contentType(APPLICATION_JSON)
            .content(body)

        // when
        val result = mockMvc.perform(request)

        // then
        result.andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errorCode").value("MISSING_REQUIRED_PARAMETER"))
            .andExpect(jsonPath("$.message").value("Required parameter '$missingField' is not present."))
        assertThat(storedSettings()).containsOnly("true")
    }

    @Test
    @DisplayName("가입 요청에 marketingAgreement가 빠지면 400 MISSING_REQUIRED_PARAMETER로 응답하고 회원이 만들어지지 않는다")
    fun signupWithoutMarketingAgreement() {
        // given
        insertVerification("device-1", MOBILE_NUMBER, verifiedAt = LocalDateTime.now().minusMinutes(5))
        val request = post("/v1/member/signup").contentType(APPLICATION_JSON).content(
            """
            {"nickname":"tester1","mobileCountry":"KR","mobileNumber":"$MOBILE_NUMBER","gender":"MALE",
             "birthDay":"1990-01-01","countryCode":"KR"}
            """.trimIndent(),
        )

        // when
        val result = mockMvc.perform(request)

        // then
        result.andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errorCode").value("MISSING_REQUIRED_PARAMETER"))
            .andExpect(jsonPath("$.message").value("Required parameter 'marketingAgreement' is not present."))
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM member", Int::class.java)).isEqualTo(0)
    }

    @Test
    @DisplayName("release-ui 변경 요청에 isRelease가 빠지면 400 MISSING_REQUIRED_PARAMETER로 응답하고 저장하지 않는다")
    fun releaseUiWithoutIsRelease() {
        // given
        insertMember(ADMIN_MEMBER_ID)
        val appVersion = Instancio.create(String::class.java)
        val request = post("/v1/admin/release-ui")
            .header(AUTHORIZATION, bearerToken(ADMIN_MEMBER_ID))
            .contentType(APPLICATION_JSON)
            .content("""{"mobileOs":"IOS","appVersion":"$appVersion"}""")

        // when
        val result = mockMvc.perform(request)

        // then
        result.andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errorCode").value("MISSING_REQUIRED_PARAMETER"))
            .andExpect(jsonPath("$.message").value("Required parameter 'isRelease' is not present."))
        assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM release_ui WHERE app_version = ?", Int::class.java, appVersion,
            ),
        ).isEqualTo(0)
    }

    private fun storedSettings(): List<String> = jdbcTemplate.queryForList(
        "SELECT value FROM member_setting, jsonb_each_text(settings) WHERE member_id = ?",
        String::class.java,
        SETTINGS_MEMBER_ID,
    )

    companion object {
        private const val SETTINGS_MEMBER_ID = 900_902L
        private const val MOBILE_NUMBER = "01012345678"
        private val SETTINGS_FIELDS = listOf("reviewCompleteNotify", "reviewRequestNotify", "marketingAgreement")
    }

}
