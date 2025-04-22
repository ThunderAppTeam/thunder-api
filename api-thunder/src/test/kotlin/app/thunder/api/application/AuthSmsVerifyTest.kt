package app.thunder.api.application

import app.thunder.api.ConcurrentRunner
import app.thunder.api.ThunderApiWebTest
import app.thunder.libraries.errors.MemberErrors.INVALID_MOBILE_VERIFICATION
import app.thunder.libraries.errors.MemberErrors.NOT_FOUND_MOBILE_VERIFICATION
import app.thunder.libraries.errors.ThunderException
import com.jayway.jsonpath.JsonPath
import java.time.Instant
import java.time.LocalDateTime
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.nullValue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class AuthSmsVerifyTest : ThunderApiWebTest() {

    @Autowired
    private lateinit var authService: AuthService

    @Test
    @DisplayName("틀린 인증번호를 입력하면 400이 반환되어도 실패 횟수가 DB에 남는다")
    fun wrongCodePersistsFailedAttempt() {
        // given
        insertVerification(DEVICE_ID, MOBILE_NUMBER, verificationCode = "123456")

        // when
        val result = mockMvc.perform(verifyRequest("000000"))

        // then
        result.andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errorCode").value("INVALID_MOBILE_VERIFICATION"))
        assertThat(failedAttemptCount()).isEqualTo(1)
    }

    @Test
    @DisplayName("틀린 인증번호를 5번 입력하면 올바른 인증번호도 429 TOO_MANY_VERIFICATION_ATTEMPTS로 거절한다")
    fun lockedAfterFiveFailures() {
        // given
        insertVerification(DEVICE_ID, MOBILE_NUMBER, verificationCode = "123456")
        repeat(5) { mockMvc.perform(verifyRequest("000000")) }

        // when
        val result = mockMvc.perform(verifyRequest("123456"))

        // then
        result.andExpect(status().isTooManyRequests)
            .andExpect(jsonPath("$.errorCode").value("TOO_MANY_VERIFICATION_ATTEMPTS"))
        assertThat(countVerifications("verified_at IS NOT NULL")).isEqualTo(0)
    }

    @Test
    @DisplayName("실패가 4번이면 올바른 인증번호로 인증할 수 있다")
    fun succeedsBeforeLimit() {
        // given
        insertVerification(DEVICE_ID, MOBILE_NUMBER, verificationCode = "123456", failedAttemptCount = 4)

        // when
        val result = mockMvc.perform(verifyRequest("123456"))

        // then
        result.andExpect(status().isOk)
        assertThat(countVerifications("verified_at IS NOT NULL")).isEqualTo(1)
    }

    @Test
    @DisplayName("같은 인증번호로 동시에 인증하면 정확히 한 요청만 성공한다")
    fun oneTimeRedemption() {
        // given
        insertVerification(DEVICE_ID, MOBILE_NUMBER, verificationCode = "123456")
        val tasks = List(8) { { authService.verifySms(DEVICE_ID, MOBILE_NUMBER, "123456") } }

        // when
        val results = ConcurrentRunner.runAll(tasks)

        // then
        assertThat(results.count { it.isSuccess }).isEqualTo(1)
        assertThat(results.mapNotNull { it.exceptionOrNull() })
            .allSatisfy {
                assertThat(it).isInstanceOf(ThunderException::class.java)
                assertThat((it as ThunderException).errorCode)
                    .isIn(INVALID_MOBILE_VERIFICATION, NOT_FOUND_MOBILE_VERIFICATION)
            }
    }

    @Test
    @DisplayName("SMS 인증으로 로그인하면 그 전에 발급된 토큰은 401 INVALID_TOKEN으로 거부된다")
    fun smsLoginRevokesPreviousTokens() {
        // given
        insertMember(MEMBER_ID)
        val previousAuthorization = bearerToken(MEMBER_ID)
        waitForNextSecond()
        insertVerification(DEVICE_ID, MOBILE_NUMBER, verificationCode = "123456")
        mockMvc.perform(verifyRequest("123456")).andExpect(status().isOk)

        // when
        val result = mockMvc.perform(get("/v1/member/info").header(AUTHORIZATION, previousAuthorization))

        // then
        result.andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.errorCode").value("INVALID_TOKEN"))
    }

    @Test
    @DisplayName("SMS 인증으로 로그인하며 받은 토큰은 바로 사용할 수 있다")
    fun smsLoginTokenIsUsable() {
        // given
        insertMember(MEMBER_ID)
        insertVerification(DEVICE_ID, MOBILE_NUMBER, verificationCode = "123456")
        val verifyResponse = mockMvc.perform(verifyRequest("123456")).andReturn().response.contentAsString
        val accessToken = JsonPath.read<String>(verifyResponse, "$.data.accessToken")

        // when
        val result = mockMvc.perform(get("/v1/member/info").header(AUTHORIZATION, "Bearer $accessToken"))

        // then
        result.andExpect(status().isOk)
    }

    @Test
    @DisplayName("휴대폰 번호의 회원이 없으면 인증은 성공하지만 토큰과 회원 ID 없이 응답하고 회원도 만들지 않는다")
    fun verifyWithoutMemberReturnsNoToken() {
        // given
        insertVerification(DEVICE_ID, MOBILE_NUMBER, verificationCode = "123456")

        // when
        val result = mockMvc.perform(verifyRequest("123456"))

        // then
        result.andExpect(status().isOk)
            .andExpect(jsonPath("$.data.accessToken").value(nullValue()))
            .andExpect(jsonPath("$.data.memberId").value(nullValue()))
        assertThat(memberCount()).isEqualTo(0)
    }

    @Test
    @DisplayName("틀린 인증번호로 실패하면 기존 회원의 로그아웃 시각은 비어 있고 이전 토큰도 계속 쓸 수 있다")
    fun wrongCodeKeepsPreviousToken() {
        // given
        val previousAuthorization = givenMemberWithPreviousToken()
        insertVerification(DEVICE_ID, MOBILE_NUMBER, verificationCode = "123456")
        mockMvc.perform(verifyRequest("000000")).andExpect(status().isBadRequest)

        // when
        val result = mockMvc.perform(get("/v1/member/info").header(AUTHORIZATION, previousAuthorization))

        // then
        result.andExpect(status().isOk)
        assertThat(loggedOutAt()).isNull()
    }

    @Test
    @DisplayName("만료된 인증으로 실패하면 기존 회원의 로그아웃 시각은 비어 있고 이전 토큰도 계속 쓸 수 있다")
    fun expiredVerificationKeepsPreviousToken() {
        // given
        val previousAuthorization = givenMemberWithPreviousToken()
        insertVerification(DEVICE_ID, MOBILE_NUMBER, verificationCode = "123456",
                           createdAt = LocalDateTime.now().minusHours(1))
        mockMvc.perform(verifyRequest("123456")).andExpect(status().is4xxClientError)

        // when
        val result = mockMvc.perform(get("/v1/member/info").header(AUTHORIZATION, previousAuthorization))

        // then
        result.andExpect(status().isOk)
        assertThat(loggedOutAt()).isNull()
    }

    @Test
    @DisplayName("실패 횟수 제한으로 잠긴 인증이 거절되면 기존 회원의 로그아웃 시각은 비어 있고 이전 토큰도 계속 쓸 수 있다")
    fun lockedVerificationKeepsPreviousToken() {
        // given
        val previousAuthorization = givenMemberWithPreviousToken()
        insertVerification(DEVICE_ID, MOBILE_NUMBER, verificationCode = "123456", failedAttemptCount = 5)
        mockMvc.perform(verifyRequest("123456")).andExpect(status().isTooManyRequests)

        // when
        val result = mockMvc.perform(get("/v1/member/info").header(AUTHORIZATION, previousAuthorization))

        // then
        result.andExpect(status().isOk)
        assertThat(loggedOutAt()).isNull()
    }

    @Test
    @DisplayName("이미 사용한 인증으로 실패하면 기존 회원의 로그아웃 시각은 비어 있고 이전 토큰도 계속 쓸 수 있다")
    fun usedVerificationKeepsPreviousToken() {
        // given
        val previousAuthorization = givenMemberWithPreviousToken()
        insertVerification(DEVICE_ID, MOBILE_NUMBER, verificationCode = "123456", verifiedAt = LocalDateTime.now())
        mockMvc.perform(verifyRequest("123456")).andExpect(status().is4xxClientError)

        // when
        val result = mockMvc.perform(get("/v1/member/info").header(AUTHORIZATION, previousAuthorization))

        // then
        result.andExpect(status().isOk)
        assertThat(loggedOutAt()).isNull()
    }

    private fun givenMemberWithPreviousToken(): String {
        insertMember(MEMBER_ID)
        val previousAuthorization = bearerToken(MEMBER_ID)
        waitForNextSecond()
        return previousAuthorization
    }

    private fun loggedOutAt(): LocalDateTime? =
        jdbcTemplate.queryForObject("SELECT logged_out_at FROM member", LocalDateTime::class.java)

    private fun memberCount(): Int =
        jdbcTemplate.queryForObject("SELECT COUNT(*) FROM member", Int::class.java)!!

    private fun waitForNextSecond() {
        val currentSecond = Instant.now().epochSecond
        while (Instant.now().epochSecond == currentSecond) {
            Thread.sleep(SECOND_POLL_MILLIS)
        }
    }

    private fun failedAttemptCount(): Int =
        jdbcTemplate.queryForObject("SELECT failed_attempt_count FROM mobile_verification", Int::class.java)!!

    private fun verifyRequest(code: String) =
        post("/v1/member/sms/verify").contentType(APPLICATION_JSON).content(
            """{"deviceId":"$DEVICE_ID","mobileNumber":"$MOBILE_NUMBER","mobileCountry":"KR","verificationCode":"$code"}""",
        )

    companion object {
        private const val DEVICE_ID = "device-1"
        private const val MEMBER_ID = 12_345_678L
        private const val MOBILE_NUMBER = "010$MEMBER_ID"
        private const val SECOND_POLL_MILLIS = 10L
    }

}
