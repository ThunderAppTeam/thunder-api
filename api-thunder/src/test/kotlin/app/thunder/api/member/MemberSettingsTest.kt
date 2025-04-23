package app.thunder.api.member

import org.assertj.core.api.Assertions.assertThat
import org.instancio.Instancio
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class MemberSettingsTest : MemberWebTest() {

    @BeforeEach
    fun createMember() {
        insertMember(MEMBER_ID)
    }

    @Test
    @DisplayName("회원 설정을 조회하면 저장된 알림 수신과 마케팅 동의 값을 그대로 돌려준다")
    fun getSettings() {
        // given
        insertSettings(MEMBER_ID, reviewCompleteNotify = false, reviewRequestNotify = true)

        // when
        val result = mockMvc.perform(get("/v1/member/settings").header(AUTHORIZATION, bearerToken(MEMBER_ID)))

        // then
        result.andExpect(status().isOk)
            .andExpect(jsonPath("$.data.reviewCompleteNotify").value(false))
            .andExpect(jsonPath("$.data.reviewRequestNotify").value(true))
            .andExpect(jsonPath("$.data.marketingAgreement").value(false))
    }

    @Test
    @DisplayName("회원 설정을 수정하면 세 값이 모두 저장되고 수정 시각이 기록된다")
    fun putSettings() {
        // given
        val reviewComplete = Instancio.create(Boolean::class.javaObjectType)
        val reviewRequest = Instancio.create(Boolean::class.javaObjectType)
        val marketing = Instancio.create(Boolean::class.javaObjectType)
        insertSettings(MEMBER_ID, reviewCompleteNotify = !reviewComplete, reviewRequestNotify = !reviewRequest)
        val request = settingsRequest(MEMBER_ID, reviewComplete, reviewRequest, marketing)

        // when
        val result = mockMvc.perform(request)

        // then
        result.andExpect(status().isOk)
        assertThat(storedSetting("reviewCompleteNotify")).isEqualTo(reviewComplete.toString())
        assertThat(storedSetting("reviewRequestNotify")).isEqualTo(reviewRequest.toString())
        assertThat(storedSetting("marketingAgreement")).isEqualTo(marketing.toString())
        assertThat(count("member_setting", "member_id = ? AND updated_at IS NOT NULL", MEMBER_ID)).isEqualTo(1)
    }

    @Test
    @DisplayName("수정한 회원 설정은 이어서 조회하면 수정한 값으로 돌려준다")
    fun getAfterPut() {
        // given
        insertSettings(MEMBER_ID)
        mockMvc.perform(settingsRequest(MEMBER_ID, false, false, true)).andExpect(status().isOk)

        // when
        val result = mockMvc.perform(get("/v1/member/settings").header(AUTHORIZATION, bearerToken(MEMBER_ID)))

        // then
        result.andExpect(status().isOk)
            .andExpect(jsonPath("$.data.reviewCompleteNotify").value(false))
            .andExpect(jsonPath("$.data.reviewRequestNotify").value(false))
            .andExpect(jsonPath("$.data.marketingAgreement").value(true))
    }

    @Test
    @DisplayName("설정이 없는 회원이 설정을 조회하면 404 NOT_FOUND_MEMBER로 응답한다")
    fun getSettingsWithoutRow() {
        // given
        val authorization = bearerToken(MEMBER_ID)

        // when
        val result = mockMvc.perform(get("/v1/member/settings").header(AUTHORIZATION, authorization))

        // then
        result.andExpect(status().isNotFound)
            .andExpect(jsonPath("$.errorCode").value("NOT_FOUND_MEMBER"))
    }

    @Test
    @DisplayName("설정이 없는 회원이 설정을 수정하면 404 NOT_FOUND_MEMBER로 응답하고 설정을 만들지 않는다")
    fun putSettingsWithoutRow() {
        // given
        val request = settingsRequest(MEMBER_ID, true, true, true)

        // when
        val result = mockMvc.perform(request)

        // then
        result.andExpect(status().isNotFound)
            .andExpect(jsonPath("$.errorCode").value("NOT_FOUND_MEMBER"))
        assertThat(count("member_setting", "member_id = ?", MEMBER_ID)).isEqualTo(0)
    }

    private fun settingsRequest(memberId: Long, reviewComplete: Boolean, reviewRequest: Boolean, marketing: Boolean) =
        put("/v1/member/settings")
            .header(AUTHORIZATION, bearerToken(memberId))
            .contentType(APPLICATION_JSON)
            .content(
                """
                {"reviewCompleteNotify":$reviewComplete,"reviewRequestNotify":$reviewRequest,"marketingAgreement":$marketing}
                """.trimIndent(),
            )

    private fun storedSetting(key: String): String? = jdbcTemplate.queryForObject(
        "SELECT settings->>'$key' FROM member_setting WHERE member_id = ?", String::class.java, MEMBER_ID,
    )

    companion object {
        private const val MEMBER_ID = 900_901L
    }

}
