package app.thunder.api.photo

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class BodyPhotoMissingTargetTest : BodyPhotoWebTest() {

    @BeforeEach
    fun createMembers() {
        insertMember(OWNER_ID)
        insertMember(ADMIN_MEMBER_ID)
    }

    @Test
    @DisplayName("존재하지 않는 사진을 지우면 404 NOT_FOUND_BODY_PHOTO로 응답하고 S3 객체를 지우지 않는다")
    fun deleteMissingPhoto() {
        // given
        val missingId = 987_654L

        // when
        val result = mockMvc.perform(
            delete("/v1/body/photo/$missingId").header(AUTHORIZATION, bearerToken(OWNER_ID)),
        )

        // then
        result.andExpect(status().isNotFound)
            .andExpect(jsonPath("$.errorCode").value("NOT_FOUND_BODY_PHOTO"))
        verify(bodyPhotoStorageClient, never()).delete(anyString())
    }

    @Test
    @DisplayName("사진 주인이 이미 없으면 결과를 조회해도 404 NOT_FOUND_MEMBER로 응답한다")
    fun resultOfPhotoWithoutOwner() {
        // given
        val bodyPhotoId = insertBodyPhoto(DELETED_OWNER_ID, reviewCount = 3, totalReviewScore = 9.0)

        // when
        val result = mockMvc.perform(
            get("/v1/body/photo/$bodyPhotoId").header(AUTHORIZATION, bearerToken(ADMIN_MEMBER_ID)),
        )

        // then
        result.andExpect(status().isNotFound)
            .andExpect(jsonPath("$.errorCode").value("NOT_FOUND_MEMBER"))
        assertThat(count("body_photo", "body_photo_id = ?", bodyPhotoId)).isEqualTo(1)
    }

    companion object {
        private const val OWNER_ID = 900_331L
        private const val DELETED_OWNER_ID = 900_332L
    }

}
