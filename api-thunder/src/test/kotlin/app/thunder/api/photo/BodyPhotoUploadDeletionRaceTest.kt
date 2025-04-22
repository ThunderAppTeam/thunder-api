package app.thunder.api.photo

import app.thunder.core.photo.domain.BodyDetectionResult
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.BDDMockito.given
import org.mockito.Mockito.verify
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class BodyPhotoUploadDeletionRaceTest : BodyPhotoWebTest() {

    @BeforeEach
    fun createMember() {
        insertMember(MEMBER_ID)
        given(bodyDetectionClient.detectBody(anyBytes())).willReturn(BodyDetectionResult(true, "Person"))
    }

    @Test
    @DisplayName("업로드 중에 회원 탈퇴가 커밋되면 404 NOT_FOUND_MEMBER로 응답하고 사진 행을 남기지 않고 올린 S3 객체를 지운다")
    fun memberDeletedDuringUpload() {
        // given
        given(bodyPhotoStorageClient.upload(anyBytes(), any(), anyText())).willAnswer {
            jdbcTemplate.update("DELETE FROM member WHERE member_id = ?", MEMBER_ID)
            IMAGE_URL
        }
        val file = MockMultipartFile("file", "photo.jpg", "image/jpeg", ByteArray(16))

        // when
        val result = mockMvc.perform(
            multipart("/v1/body/photo").file(file).header(AUTHORIZATION, bearerToken(MEMBER_ID)),
        )

        // then
        result.andExpect(status().isNotFound)
            .andExpect(jsonPath("$.errorCode").value("NOT_FOUND_MEMBER"))
        assertThat(count("body_photo", "member_id = ?", MEMBER_ID)).isEqualTo(0)
        verify(bodyPhotoStorageClient).delete(IMAGE_URL)
    }

    companion object {
        private const val MEMBER_ID = 900_061L
        private const val IMAGE_URL = "https://test-bucket.s3.ap-northeast-2.amazonaws.com/900061/body_photo/race.jpg"
    }

}
