package app.thunder.api.application

import app.thunder.api.ThunderApiWebTest
import app.thunder.core.member.client.SmsClient
import app.thunder.libraries.errors.ExternalApiException
import app.thunder.libraries.errors.ExternalErrors.SEND_SMS_API_ERROR
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyString
import org.mockito.BDDMockito.willThrow
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class AuthSmsExternalErrorTest : ThunderApiWebTest() {

    @MockitoBean
    private lateinit var smsClient: SmsClient

    @Test
    @DisplayName("SMS 발송 API가 실패하면 500 SEND_SMS_API_ERROR로 응답하고 인증 기록을 남기지 않는다")
    fun smsApiFailure() {
        // given
        willThrow(ExternalApiException(SEND_SMS_API_ERROR)).given(smsClient).sendSms(anyString(), anyString(), anyBoolean())
        val request = post("/v1/member/sms").contentType(APPLICATION_JSON)
            .content("""{"deviceId":"$DEVICE_ID","mobileNumber":"$MOBILE_NUMBER","mobileCountry":"KR"}""")

        // when
        val result = mockMvc.perform(request)

        // then
        result.andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.errorCode").value("SEND_SMS_API_ERROR"))
            .andExpect(jsonPath("$.message").value(SEND_SMS_API_ERROR.message))
            .andExpect(jsonPath("$.path").value("/v1/member/sms"))
        assertThat(countVerifications("device_id = ?", DEVICE_ID)).isEqualTo(0)
    }

    companion object {
        private const val DEVICE_ID = "device-1"
        private const val MOBILE_NUMBER = "01012345678"
    }

}
