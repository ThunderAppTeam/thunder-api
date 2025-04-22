package app.thunder.api.photo

import org.assertj.core.api.Assertions.assertThat
import java.util.concurrent.atomic.AtomicInteger
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.BDDMockito.willAnswer
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class BodyPhotoDeleteTest : BodyPhotoWebTest() {

    @BeforeEach
    fun createMembers() {
        insertMember(OWNER_ID)
        insertMember(OTHER_ID)
    }

    @Test
    @DisplayName("사진을 지우면 그 사진의 리뷰와 신고 기록도 함께 지우고 다른 사진의 기록은 남긴다")
    fun deleteRemovesReviewsAndFlags() {
        // given
        val imageUrl = "https://test-bucket.s3.ap-northeast-2.amazonaws.com/$OWNER_ID/body_photo/target.jpg"
        val target = insertBodyPhoto(OWNER_ID, imageUrl = imageUrl)
        val kept = insertBodyPhoto(OWNER_ID)
        insertBodyReview(target, OTHER_ID, 4)
        insertFlagHistory(target, OTHER_ID)
        insertBodyReview(kept, OTHER_ID, 3)
        insertFlagHistory(kept, OTHER_ID)

        // when
        val result = mockMvc.perform(
            delete("/v1/body/photo/$target").header(AUTHORIZATION, bearerToken(OWNER_ID)),
        )

        // then
        result.andExpect(status().isOk)
        assertThat(count("body_photo", "body_photo_id = ?", target)).isEqualTo(0)
        assertThat(count("body_review", "body_photo_id = ?", target)).isEqualTo(0)
        assertThat(count("flag_history", "body_photo_id = ?", target)).isEqualTo(0)
        assertThat(count("body_photo", "body_photo_id = ?", kept)).isEqualTo(1)
        assertThat(count("body_review", "body_photo_id = ?", kept)).isEqualTo(1)
        assertThat(count("flag_history", "body_photo_id = ?", kept)).isEqualTo(1)
        verify(bodyPhotoStorageClient).delete(imageUrl)
    }

    @Test
    @DisplayName("S3 객체는 사진 삭제가 커밋된 뒤에 지운다")
    fun storageDeletionHappensAfterCommit() {
        // given
        val target = insertBodyPhoto(OWNER_ID)
        val rowCountWhenDeleting = AtomicInteger(-1)
        willAnswer {
            rowCountWhenDeleting.set(count("body_photo", "body_photo_id = ?", target))
        }.given(bodyPhotoStorageClient).delete(anyText())

        // when
        mockMvc.perform(delete("/v1/body/photo/$target").header(AUTHORIZATION, bearerToken(OWNER_ID)))

        // then
        assertThat(rowCountWhenDeleting.get()).isEqualTo(0)
    }

    @Test
    @DisplayName("업로더가 아닌 회원이 사진을 지우면 403으로 응답하고 아무것도 지우지 않는다")
    fun nonUploaderCannotDelete() {
        // given
        val target = insertBodyPhoto(OWNER_ID)
        insertBodyReview(target, OTHER_ID, 4)

        // when
        val result = mockMvc.perform(
            delete("/v1/body/photo/$target").header(AUTHORIZATION, bearerToken(OTHER_ID)),
        )

        // then
        result.andExpect(status().isForbidden)
            .andExpect(jsonPath("$.errorCode").value("UPLOADER_OR_ADMIN_ONLY_ACCESS"))
        assertThat(count("body_photo", "body_photo_id = ?", target)).isEqualTo(1)
        assertThat(count("body_review", "body_photo_id = ?", target)).isEqualTo(1)
        verify(bodyPhotoStorageClient, never()).delete(anyString())
    }

    companion object {
        private const val OWNER_ID = 900_031L
        private const val OTHER_ID = 900_032L
    }

}
