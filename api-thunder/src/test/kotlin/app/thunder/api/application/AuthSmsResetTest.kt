package app.thunder.api.application

import app.thunder.api.ThunderApiWebTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class AuthSmsResetTest : ThunderApiWebTest() {

    @BeforeEach
    fun createAdmin() {
        insertMember(ADMIN_MEMBER_ID)
    }

    @Test
    @DisplayName("기기 ID와 휴대폰 번호를 함께 보내면 둘 다 초기화한다")
    fun resetsBothDeviceAndMobileNumber() {
        // given
        insertVerification("device-1", "01011110000")
        insertVerification("device-2", "01022220000")
        insertVerification("device-3", "01033330000")

        // when
        val result = mockMvc.perform(resetRequest("""{"deviceId":"device-1","mobileNumber":"01022220000"}"""))

        // then
        result.andExpect(status().isOk)
        assertThat(countVerifications("verified_at IS NULL")).isEqualTo(1)
        assertThat(countVerifications("verified_at IS NULL AND device_id = ?", "device-3")).isEqualTo(1)
    }

    @Test
    @DisplayName("기기 ID만 보내면 그 기기만 초기화한다")
    fun resetsDeviceOnly() {
        // given
        insertVerification("device-1", "01011110000")
        insertVerification("device-2", "01022220000")

        // when
        val result = mockMvc.perform(resetRequest("""{"deviceId":"device-1"}"""))

        // then
        result.andExpect(status().isOk)
        assertThat(countVerifications("verified_at IS NULL AND device_id = ?", "device-2")).isEqualTo(1)
        assertThat(countVerifications("verified_at IS NULL")).isEqualTo(1)
    }

    @Test
    @DisplayName("기기 ID도 휴대폰 번호도 없으면 400으로 응답한다")
    fun missingBoth() {
        // given
        val request = resetRequest("{}")

        // when
        val result = mockMvc.perform(request)

        // then
        result.andExpect(status().isBadRequest)
    }

    private fun resetRequest(body: String) =
        post("/v1/member/sms/reset").header(AUTHORIZATION, bearerToken(ADMIN_MEMBER_ID))
            .contentType(APPLICATION_JSON).content(body)

}
