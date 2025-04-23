package app.thunder.api.photo

import java.time.LocalDateTime
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class BodyPhotoListTest : BodyPhotoWebTest() {

    @BeforeEach
    fun createMembers() {
        insertMember(OWNER_ID)
        insertMember(OTHER_ID)
    }

    @Test
    @DisplayName("내 사진 목록을 조회하면 사진마다 리뷰 수와 환산 점수와 완료 여부를 응답한다")
    fun listOwnPhotos() {
        // given
        val completed = insertBodyPhoto(OWNER_ID, reviewCount = 10, totalReviewScore = 40.0)
        val inProgress = insertBodyPhoto(OWNER_ID, reviewCount = 4, totalReviewScore = 14.0)
        val notReviewed = insertBodyPhoto(OWNER_ID)

        // when
        val result = mockMvc.perform(get("/v1/body/photo").header(AUTHORIZATION, bearerToken(OWNER_ID)))

        // then
        result.andExpect(status().isOk)
            .andExpect(jsonPath("$.data.length()").value(3))
            .andExpect(jsonPath("$.data[?(@.bodyPhotoId==$completed)].isReviewCompleted").value(true))
            .andExpect(jsonPath("$.data[?(@.bodyPhotoId==$completed)].reviewCount").value(10))
            .andExpect(jsonPath("$.data[?(@.bodyPhotoId==$completed)].reviewScore").value(8.0))
            .andExpect(jsonPath("$.data[?(@.bodyPhotoId==$inProgress)].isReviewCompleted").value(false))
            .andExpect(jsonPath("$.data[?(@.bodyPhotoId==$inProgress)].reviewCount").value(4))
            .andExpect(jsonPath("$.data[?(@.bodyPhotoId==$inProgress)].reviewScore").value(7.0))
            .andExpect(jsonPath("$.data[?(@.bodyPhotoId==$notReviewed)].reviewCount").value(0))
            .andExpect(jsonPath("$.data[?(@.bodyPhotoId==$notReviewed)].reviewScore").value(0.0))
    }

    @Test
    @DisplayName("사진 목록에는 사진의 이미지 주소와 생성 시각이 한국 시간으로 담긴다")
    fun listIncludesImageUrlAndKoreaCreatedAt() {
        // given
        val imageUrl = "https://test-bucket.s3.ap-northeast-2.amazonaws.com/$OWNER_ID/body_photo/listed.jpg"
        insertBodyPhoto(OWNER_ID, imageUrl = imageUrl, createdAt = LocalDateTime.of(2026, 1, 2, 3, 4, 5))

        // when
        val result = mockMvc.perform(get("/v1/body/photo").header(AUTHORIZATION, bearerToken(OWNER_ID)))

        // then
        result.andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].imageUrl").value(imageUrl))
            .andExpect(jsonPath("$.data[0].createdAt").value("2026-01-02T12:04:05+09:00"))
    }

    @Test
    @DisplayName("다른 회원의 사진은 내 사진 목록에 나오지 않는다")
    fun listExcludesOtherMembersPhotos() {
        // given
        insertBodyPhoto(OTHER_ID)

        // when
        val result = mockMvc.perform(get("/v1/body/photo").header(AUTHORIZATION, bearerToken(OWNER_ID)))

        // then
        result.andExpect(status().isOk)
            .andExpect(jsonPath("$.data.length()").value(0))
    }

    companion object {
        private const val OWNER_ID = 900_311L
        private const val OTHER_ID = 900_312L
    }

}
