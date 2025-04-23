package app.thunder.api.exception

import app.thunder.api.ThunderApiWebTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class GlobalExceptionHandlerParameterTest : ThunderApiWebTest() {

    @Test
    @DisplayName("요청 본문에 필수 필드가 빠지면 400 MISSING_REQUIRED_PARAMETER로 응답하고 빠진 필드 이름을 알려준다")
    fun missingBodyField() {
        // given
        val request = post("/v1/member/sms").contentType(APPLICATION_JSON)
            .content("""{"deviceId":"device-1","mobileCountry":"KR"}""")

        // when
        val result = mockMvc.perform(request)

        // then
        result.andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errorCode").value("MISSING_REQUIRED_PARAMETER"))
            .andExpect(jsonPath("$.message").value("Required parameter 'mobileNumber' is not present."))
            .andExpect(jsonPath("$.path").value("/v1/member/sms"))
    }

    @Test
    @DisplayName("요청 본문이 객체가 아니라 배열이면 필드 이름 없이 400 MISSING_REQUIRED_PARAMETER 기본 메시지로 응답한다")
    fun bodyIsNotAnObject() {
        // given
        val request = post("/v1/member/sms").contentType(APPLICATION_JSON).content("[]")

        // when
        val result = mockMvc.perform(request)

        // then
        result.andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errorCode").value("MISSING_REQUIRED_PARAMETER"))
            .andExpect(jsonPath("$.message").value("Invalid request body format."))
    }

}
