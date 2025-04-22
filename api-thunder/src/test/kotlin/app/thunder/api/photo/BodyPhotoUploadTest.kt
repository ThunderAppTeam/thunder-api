package app.thunder.api.photo

import app.thunder.core.photo.domain.BodyDetectionResult
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.util.Random
import javax.imageio.ImageIO
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.ArgumentMatchers.any
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mockingDetails
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class BodyPhotoUploadTest : BodyPhotoWebTest() {

    @BeforeEach
    fun prepare() {
        insertMember(MEMBER_ID)
        given(bodyDetectionClient.detectBody(anyBytes())).willReturn(BodyDetectionResult(true, "Person"))
        given(bodyPhotoStorageClient.upload(anyBytes(), any(), anyText())).willAnswer {
            "$URL_PREFIX${it.getArgument<String>(2)}"
        }
    }

    @ParameterizedTest
    @CsvSource("image/jpeg,jpg", "image/png,png")
    @DisplayName("업로드한 사진의 저장 경로는 회원 ID와 UUID와 확장자로만 만들어진다")
    fun storageKeyFormat(contentType: String, extension: String) {
        // given
        val file = MockMultipartFile("file", "내 사진 (1).$extension", contentType, noiseImage(8, 8, extension))

        // when
        val result = mockMvc.perform(
            multipart("/v1/body/photo").file(file).header(AUTHORIZATION, bearerToken(MEMBER_ID)),
        )

        // then
        val filePath = uploadedFilePaths().single()
        assertThat(filePath).matches("^$MEMBER_ID/body_photo/[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}\\.$extension$")
        result.andExpect(status().isOk)
            .andExpect(jsonPath("$.data.imageUrl").value("$URL_PREFIX$filePath"))
        assertThat(count("body_photo", "image_url = ?", "$URL_PREFIX$filePath")).isEqualTo(1)
    }

    @Test
    @DisplayName("5MB 이상 사진은 최적화한 뒤 같은 바이트로 신체 감지와 저장을 한다")
    fun largeImageIsOptimizedBeforeDetection() {
        // given
        val original = noiseImage(1500, 1500, "png")
        val file = MockMultipartFile("file", "large.png", "image/png", original)

        // when
        val result = mockMvc.perform(
            multipart("/v1/body/photo").file(file).header(AUTHORIZATION, bearerToken(MEMBER_ID)),
        )

        // then
        result.andExpect(status().isOk)
        val detected = detectedBytes().single()
        val stored = uploadedBytes().single()
        assertThat(original.size).isGreaterThanOrEqualTo(FIVE_MB)
        assertThat(detected.size).isLessThan(FIVE_MB)
        assertThat(detected).isEqualTo(stored)
    }

    @Test
    @DisplayName("최적화 후에도 5MB 이상이면 400 IMAGE_TOO_LARGE로 응답하고 감지와 저장을 하지 않는다")
    fun imageStillTooLargeAfterOptimization() {
        // given
        val file = MockMultipartFile("file", "huge.png", "image/png", noiseImage(1080, 1920, "png"))

        // when
        val result = mockMvc.perform(
            multipart("/v1/body/photo").file(file).header(AUTHORIZATION, bearerToken(MEMBER_ID)),
        )

        // then
        result.andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errorCode").value("IMAGE_TOO_LARGE"))
        assertThat(detectedBytes()).isEmpty()
        assertThat(uploadedBytes()).isEmpty()
        assertThat(count("body_photo", "member_id = ?", MEMBER_ID)).isEqualTo(0)
    }

    private fun noiseImage(width: Int, height: Int, format: String): ByteArray {
        val random = Random(SEED)
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        for (y in 0 until height) {
            for (x in 0 until width) {
                image.setRGB(x, y, random.nextInt(0x1000000))
            }
        }
        val output = ByteArrayOutputStream()
        ImageIO.write(image, format, output)
        return output.toByteArray()
    }

    private fun uploadedFilePaths(): List<String> = invocationArguments(bodyPhotoStorageClient, "upload").map { it[2] as String }

    private fun uploadedBytes(): List<ByteArray> = invocationArguments(bodyPhotoStorageClient, "upload").map { it[0] as ByteArray }

    private fun detectedBytes(): List<ByteArray> = invocationArguments(bodyDetectionClient, "detectBody").map { it[0] as ByteArray }

    private fun invocationArguments(mock: Any, methodName: String): List<Array<Any?>> =
        mockingDetails(mock).invocations.filter { it.method.name == methodName }.map { it.arguments }

    companion object {
        private const val MEMBER_ID = 900_021L
        private const val SEED = 7L
        private const val FIVE_MB = 5 * 1024 * 1024
        private const val URL_PREFIX = "https://test-bucket.s3.ap-northeast-2.amazonaws.com/"
    }

}
