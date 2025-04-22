package app.thunder.api.application

import app.thunder.api.ThunderApiWebTest
import java.time.LocalDate
import java.time.LocalDateTime
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class AuthSignupTest : ThunderApiWebTest() {

    @Test
    @DisplayName("휴대폰 인증 기록이 없는 번호로 가입하면 403 MOBILE_NOT_VERIFIED로 응답하고 회원이 만들어지지 않는다")
    fun signupWithoutVerification() {
        // given
        val request = signupRequest()

        // when
        val result = mockMvc.perform(request)

        // then
        result.andExpect(status().isForbidden)
            .andExpect(jsonPath("$.errorCode").value("MOBILE_NOT_VERIFIED"))
        assertThat(memberCount()).isEqualTo(0)
    }

    @Test
    @DisplayName("인증만 요청하고 완료하지 않은 번호로 가입하면 403 MOBILE_NOT_VERIFIED로 응답한다")
    fun signupWithUnverifiedRequest() {
        // given
        insertVerification("device-1", MOBILE_NUMBER)

        // when
        val result = mockMvc.perform(signupRequest())

        // then
        result.andExpect(status().isForbidden)
            .andExpect(jsonPath("$.errorCode").value("MOBILE_NOT_VERIFIED"))
    }

    @Test
    @DisplayName("30분보다 오래 전에 인증한 번호로 가입하면 403 MOBILE_NOT_VERIFIED로 응답한다")
    fun signupWithStaleVerification() {
        // given
        insertVerification("device-1", MOBILE_NUMBER, verifiedAt = LocalDateTime.now().minusMinutes(31))

        // when
        val result = mockMvc.perform(signupRequest())

        // then
        result.andExpect(status().isForbidden)
            .andExpect(jsonPath("$.errorCode").value("MOBILE_NOT_VERIFIED"))
        assertThat(memberCount()).isEqualTo(0)
    }

    @Test
    @DisplayName("30분 안에 인증한 번호로 가입하면 회원이 만들어지고 토큰이 발급된다")
    fun signupWithRecentVerification() {
        // given
        insertVerification("device-1", MOBILE_NUMBER, verifiedAt = LocalDateTime.now().minusMinutes(5))

        // when
        val result = mockMvc.perform(signupRequest())

        // then
        result.andExpect(status().isOk)
            .andExpect(jsonPath("$.data.accessToken").isNotEmpty)
        assertThat(memberCount()).isEqualTo(1)
    }

    @ParameterizedTest
    @ValueSource(strings = ["010-1234-5678", "+821012345678", "abcdefghijk", "010123456"])
    @DisplayName("숫자 10~15자리가 아닌 휴대폰 번호로 가입하면 400으로 응답한다")
    fun invalidMobileNumber(mobileNumber: String) {
        // given
        val request = signupRequest(mobileNumber = mobileNumber)

        // when
        val result = mockMvc.perform(request)

        // then
        result.andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errorCode").value("INVALID_PARAMETER_VALUE"))
    }

    @Test
    @DisplayName("생년월일이 오늘이거나 미래이면 400으로 응답한다")
    fun futureBirthDay() {
        // given
        insertVerification("device-1", MOBILE_NUMBER, verifiedAt = LocalDateTime.now())
        val request = signupRequest(birthDay = LocalDate.now().plusDays(1).toString())

        // when
        val result = mockMvc.perform(request)

        // then
        result.andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errorCode").value("INVALID_PARAMETER_VALUE"))
        assertThat(memberCount()).isEqualTo(0)
    }

    private fun memberCount(): Int =
        jdbcTemplate.queryForObject("SELECT COUNT(*) FROM member", Int::class.java)!!

    private fun signupRequest(mobileNumber: String = MOBILE_NUMBER, birthDay: String = "1990-01-01") =
        post("/v1/member/signup").contentType(APPLICATION_JSON).content(
            """
            {"nickname":"tester1","mobileCountry":"KR","mobileNumber":"$mobileNumber","gender":"MALE",
             "birthDay":"$birthDay","countryCode":"KR","marketingAgreement":false}
            """.trimIndent(),
        )

    companion object {
        private const val MOBILE_NUMBER = "01012345678"
    }

}
