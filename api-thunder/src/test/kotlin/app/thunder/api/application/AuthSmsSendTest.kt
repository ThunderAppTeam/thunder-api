package app.thunder.api.application

import app.thunder.api.ConcurrentRunner
import app.thunder.api.ThunderApiWebTest
import app.thunder.api.controller.request.PostSmsRequest
import app.thunder.core.member.client.SmsClient
import app.thunder.libraries.errors.MemberErrors.TOO_MANY_MOBILE_VERIFICATION
import app.thunder.libraries.errors.ThunderException
import java.time.LocalDateTime
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.doAnswer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class AuthSmsSendTest : ThunderApiWebTest() {

    @MockitoBean
    private lateinit var smsClient: SmsClient

    @Autowired
    private lateinit var authService: AuthService

    @Test
    @DisplayName("같은 휴대폰 번호로 24시간 안에 미인증 발송이 5번 있으면 다른 기기에서도 429 TOO_MANY_MOBILE_VERIFICATION으로 응답한다")
    fun mobileNumberDailyLimit() {
        // given
        repeat(5) { insertVerification("device-$it", MOBILE_NUMBER) }

        // when
        val result = mockMvc.perform(sendSmsRequest("new-device", MOBILE_NUMBER))

        // then
        result.andExpect(status().isTooManyRequests)
            .andExpect(jsonPath("$.errorCode").value("TOO_MANY_MOBILE_VERIFICATION"))
    }

    @Test
    @DisplayName("인증이 끝난 발송은 기기와 휴대폰 번호 어느 한도에도 세지 않는다")
    fun verifiedSendsAreNotCounted() {
        // given
        repeat(5) { insertVerification(DEVICE_ID, MOBILE_NUMBER, verifiedAt = LocalDateTime.now()) }

        // when
        val result = mockMvc.perform(sendSmsRequest(DEVICE_ID, MOBILE_NUMBER))

        // then
        result.andExpect(status().isOk)
    }

    @Test
    @DisplayName("24시간이 지난 발송은 휴대폰 번호 한도에 세지 않는다")
    fun oldSendsAreNotCounted() {
        // given
        repeat(5) { insertVerification("device-$it", MOBILE_NUMBER, createdAt = LocalDateTime.now().minusDays(2)) }

        // when
        val result = mockMvc.perform(sendSmsRequest("new-device", MOBILE_NUMBER))

        // then
        result.andExpect(status().isOk)
    }

    @Test
    @DisplayName("같은 기기에서 동시에 발송을 요청하면 이미 4번 보낸 경우 정확히 한 요청만 성공한다")
    fun concurrentSendsFromSameDevice() {
        // given
        repeat(4) { insertVerification(DEVICE_ID, "0101111000$it") }
        slowDownSmsClient()

        // when
        val results = ConcurrentRunner.runAll(
            listOf(
                { authService.sendSms(smsRequest(DEVICE_ID, "01022220001")) },
                { authService.sendSms(smsRequest(DEVICE_ID, "01022220002")) },
            ),
        )

        // then
        assertThat(results.count { it.isSuccess }).isEqualTo(1)
        assertThat(results.mapNotNull { it.exceptionOrNull() })
            .singleElement()
            .isInstanceOfSatisfying(ThunderException::class.java) {
                assertThat(it.errorCode).isEqualTo(TOO_MANY_MOBILE_VERIFICATION)
            }
        assertThat(countVerifications("device_id = ?", DEVICE_ID)).isEqualTo(5)
    }

    @Test
    @DisplayName("서로 다른 기기에서 같은 번호로 동시에 발송을 요청하면 이미 4번 보낸 경우 정확히 한 요청만 성공한다")
    fun concurrentSendsToSameNumber() {
        // given
        repeat(4) { insertVerification("device-$it", MOBILE_NUMBER) }
        slowDownSmsClient()

        // when
        val results = ConcurrentRunner.runAll(
            listOf(
                { authService.sendSms(smsRequest("device-a", MOBILE_NUMBER)) },
                { authService.sendSms(smsRequest("device-b", MOBILE_NUMBER)) },
            ),
        )

        // then
        assertThat(results.count { it.isSuccess }).isEqualTo(1)
        assertThat(countVerifications("mobile_number = ?", MOBILE_NUMBER)).isEqualTo(5)
    }

    @ParameterizedTest
    @ValueSource(strings = ["010-1234-5678", "+821012345678", "010123456", "0101234567890123", "abcdefghijk"])
    @DisplayName("숫자 10~15자리가 아닌 휴대폰 번호로 인증번호를 요청하면 400으로 응답한다")
    fun invalidMobileNumberOnSend(mobileNumber: String) {
        // given
        val request = sendSmsRequest(DEVICE_ID, mobileNumber)

        // when
        val result = mockMvc.perform(request)

        // then
        result.andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errorCode").value("INVALID_PARAMETER_VALUE"))
    }

    @ParameterizedTest
    @ValueSource(strings = ["010-1234-5678", "+821012345678", "abcdefghijk"])
    @DisplayName("숫자 10~15자리가 아닌 휴대폰 번호로 인증을 요청하면 400으로 응답한다")
    fun invalidMobileNumberOnVerify(mobileNumber: String) {
        // given
        val body = """{"deviceId":"$DEVICE_ID","mobileNumber":"$mobileNumber","mobileCountry":"KR","verificationCode":"123456"}"""

        // when
        val result = mockMvc.perform(post("/v1/member/sms/verify").contentType(APPLICATION_JSON).content(body))

        // then
        result.andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errorCode").value("INVALID_PARAMETER_VALUE"))
    }

    private fun slowDownSmsClient() {
        doAnswer { Thread.sleep(SMS_DELAY_MILLIS) }
            .`when`(smsClient).sendSms(anyString(), anyString(), anyBoolean())
    }

    private fun smsRequest(deviceId: String, mobileNumber: String) =
        PostSmsRequest(deviceId = deviceId, mobileNumber = mobileNumber, mobileCountry = "KR")

    private fun sendSmsRequest(deviceId: String, mobileNumber: String) =
        post("/v1/member/sms").contentType(APPLICATION_JSON)
            .content("""{"deviceId":"$deviceId","mobileNumber":"$mobileNumber","mobileCountry":"KR"}""")

    companion object {
        private const val DEVICE_ID = "device-1"
        private const val MOBILE_NUMBER = "01012345678"
        private const val SMS_DELAY_MILLIS = 400L
    }

}
