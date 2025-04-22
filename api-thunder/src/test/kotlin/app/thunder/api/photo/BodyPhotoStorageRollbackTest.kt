package app.thunder.api.photo

import app.thunder.core.photo.domain.BodyDetectionResult
import app.thunder.core.photo.domain.BodyPhoto
import app.thunder.core.photo.repository.BodyPhotoRepository
import app.thunder.core.review.repository.ReviewableBodyPhotoRepository
import app.thunder.libraries.errors.CommonErrors.UNKNOWN_SERVER_ERROR
import app.thunder.libraries.errors.ThunderException
import org.instancio.Instancio
import org.instancio.Select.field
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.anyString
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.willThrow
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class BodyPhotoStorageRollbackTest : BodyPhotoWebTest() {

    @MockitoBean
    private lateinit var bodyPhotoRepository: BodyPhotoRepository

    @MockitoBean
    private lateinit var reviewableBodyPhotoRepository: ReviewableBodyPhotoRepository

    @BeforeEach
    fun createMember() {
        insertMember(MEMBER_ID)
    }

    @Test
    @DisplayName("업로드 후 저장에 실패해 롤백되면 방금 올린 S3 객체를 지운다")
    fun uploadedObjectIsDeletedOnRollback() {
        // given
        given(bodyDetectionClient.detectBody(anyBytes())).willReturn(BodyDetectionResult(true, "Person"))
        given(bodyPhotoStorageClient.upload(anyBytes(), any(), anyText())).willReturn(IMAGE_URL)
        given(bodyPhotoRepository.create(anyLong(), anyText())).willThrow(ThunderException(UNKNOWN_SERVER_ERROR))
        val file = MockMultipartFile("file", "photo.jpg", "image/jpeg", ByteArray(16))

        // when
        val result = mockMvc.perform(
            multipart("/v1/body/photo").file(file).header(AUTHORIZATION, bearerToken(MEMBER_ID)),
        )

        // then
        result.andExpect(status().is5xxServerError)
        verify(bodyPhotoStorageClient).delete(IMAGE_URL)
    }

    @Test
    @DisplayName("사진 삭제가 롤백되면 S3 객체를 지우지 않는다")
    fun objectIsKeptWhenDeleteRollsBack() {
        // given
        val bodyPhoto = Instancio.of(BodyPhoto::class.java)
            .set(field("bodyPhotoId"), BODY_PHOTO_ID)
            .set(field("memberId"), MEMBER_ID)
            .set(field("imageUrl"), IMAGE_URL)
            .create()
        given(bodyPhotoRepository.getById(BODY_PHOTO_ID)).willReturn(bodyPhoto)
        willThrow(ThunderException(UNKNOWN_SERVER_ERROR))
            .given(reviewableBodyPhotoRepository).deleteAllByBodyPhotoId(BODY_PHOTO_ID)

        // when
        val result = mockMvc.perform(
            delete("/v1/body/photo/$BODY_PHOTO_ID").header(AUTHORIZATION, bearerToken(MEMBER_ID)),
        )

        // then
        result.andExpect(status().is5xxServerError)
        verify(bodyPhotoStorageClient, never()).delete(anyString())
    }

    companion object {
        private const val MEMBER_ID = 900_051L
        private const val BODY_PHOTO_ID = 4_242L
        private const val IMAGE_URL = "https://test-bucket.s3.ap-northeast-2.amazonaws.com/900051/body_photo/rollback.jpg"
    }

}
