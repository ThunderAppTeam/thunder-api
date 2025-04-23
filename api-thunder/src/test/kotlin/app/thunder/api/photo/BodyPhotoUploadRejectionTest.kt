package app.thunder.api.photo

import app.thunder.core.photo.domain.BodyDetectionResult
import org.assertj.core.api.Assertions.assertThat
import org.instancio.Instancio
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mockingDetails
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class BodyPhotoUploadRejectionTest : BodyPhotoWebTest() {

    @BeforeEach
    fun createMember() {
        insertMember(MEMBER_ID)
    }

    @Test
    @DisplayName("JPG와 PNG가 아닌 형식을 올리면 400 UNSUPPORTED_IMAGE_FORMAT으로 응답하고 감지와 저장을 하지 않는다")
    fun unsupportedImageFormat() {
        // given
        val file = MockMultipartFile("file", "animated.gif", "image/gif", Instancio.create(ByteArray::class.java))

        // when
        val result = mockMvc.perform(
            multipart("/v1/body/photo").file(file).header(AUTHORIZATION, bearerToken(MEMBER_ID)),
        )

        // then
        result.andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errorCode").value("UNSUPPORTED_IMAGE_FORMAT"))
        assertThat(invocationCount(bodyDetectionClient, "detectBody")).isEqualTo(0)
        assertThat(invocationCount(bodyPhotoStorageClient, "upload")).isEqualTo(0)
        assertThat(count("body_photo", "member_id = ?", MEMBER_ID)).isEqualTo(0)
    }

    @Test
    @DisplayName("사진에서 신체가 감지되지 않으면 400 BODY_NOT_DETECTED_IN_PHOTO로 응답하고 저장하지 않는다")
    fun bodyNotDetected() {
        // given
        given(bodyDetectionClient.detectBody(anyBytes())).willReturn(BodyDetectionResult(false, "no person"))
        val file = MockMultipartFile("file", "landscape.jpg", "image/jpeg", Instancio.create(ByteArray::class.java))

        // when
        val result = mockMvc.perform(
            multipart("/v1/body/photo").file(file).header(AUTHORIZATION, bearerToken(MEMBER_ID)),
        )

        // then
        result.andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errorCode").value("BODY_NOT_DETECTED_IN_PHOTO"))
        assertThat(invocationCount(bodyDetectionClient, "detectBody")).isEqualTo(1)
        assertThat(invocationCount(bodyPhotoStorageClient, "upload")).isEqualTo(0)
        assertThat(count("body_photo", "member_id = ?", MEMBER_ID)).isEqualTo(0)
    }

    private fun invocationCount(mock: Any, methodName: String): Int =
        mockingDetails(mock).invocations.count { it.method.name == methodName }

    companion object {
        private const val MEMBER_ID = 900_321L
    }

}
