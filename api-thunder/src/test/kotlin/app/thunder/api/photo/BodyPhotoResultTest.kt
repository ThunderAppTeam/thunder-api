package app.thunder.api.photo

import java.time.LocalDateTime
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class BodyPhotoResultTest : BodyPhotoWebTest() {

    @BeforeEach
    fun createMembers() {
        insertMemberWithGender(ADMIN_MEMBER_ID, "MALE")
        insertMemberWithGender(OWNER_ID, "FEMALE")
        insertMemberWithGender(OTHER_ID, "MALE")
        insertMemberWithGender(MALE_PEER_ID, "MALE")
        insertMemberWithGender(FEMALE_PEER_ID, "FEMALE")
    }

    @Test
    @DisplayName("업로더가 결과를 조회하면 진행률은 리뷰 10개를 기준으로 계산된다")
    fun progressRateUsesCompleteCount() {
        // given
        val bodyPhotoId = insertBodyPhoto(OWNER_ID, reviewCount = 5, totalReviewScore = 20.0)

        // when
        val result = mockMvc.perform(
            get("/v1/body/photo/$bodyPhotoId").header(AUTHORIZATION, bearerToken(OWNER_ID)),
        )

        // then
        result.andExpect(status().isOk)
            .andExpect(jsonPath("$.data.progressRate").value(50.0))
            .andExpect(jsonPath("$.data.isReviewCompleted").value(false))
    }

    @Test
    @DisplayName("업로더가 아닌 회원이 결과를 조회하면 403 UPLOADER_OR_ADMIN_ONLY_ACCESS로 응답한다")
    fun nonUploaderCannotReadResult() {
        // given
        val bodyPhotoId = insertBodyPhoto(OWNER_ID)

        // when
        val result = mockMvc.perform(
            get("/v1/body/photo/$bodyPhotoId").header(AUTHORIZATION, bearerToken(OTHER_ID)),
        )

        // then
        result.andExpect(status().isForbidden)
            .andExpect(jsonPath("$.errorCode").value("UPLOADER_OR_ADMIN_ONLY_ACCESS"))
    }

    @Test
    @DisplayName("존재하지 않는 사진의 결과를 조회하면 404 NOT_FOUND_BODY_PHOTO로 응답한다")
    fun missingPhotoResult() {
        // given
        val missingId = 987_654L

        // when
        val result = mockMvc.perform(
            get("/v1/body/photo/$missingId").header(AUTHORIZATION, bearerToken(OWNER_ID)),
        )

        // then
        result.andExpect(status().isNotFound)
            .andExpect(jsonPath("$.errorCode").value("NOT_FOUND_BODY_PHOTO"))
    }

    @Test
    @DisplayName("관리자가 다른 회원의 결과를 조회하면 사진 주인의 성별로 응답한다")
    fun adminReadsResultWithOwnerGender() {
        // given
        val bodyPhotoId = insertBodyPhoto(OWNER_ID, reviewCount = 1, totalReviewScore = 5.0)

        // when
        val result = mockMvc.perform(
            get("/v1/body/photo/$bodyPhotoId").header(AUTHORIZATION, bearerToken(ADMIN_MEMBER_ID)),
        )

        // then
        result.andExpect(status().isOk)
            .andExpect(jsonPath("$.data.gender").value("FEMALE"))
            .andExpect(jsonPath("$.data.genderTopRate").value(100.0))
    }

    @Test
    @DisplayName("순위는 같은 성별의 최근 30일 리뷰 완료 사진 중 평균 점수가 더 높은 사진 수로 계산된다")
    fun rankingUsesCompletedPhotosWithinCohort() {
        // given
        val target = insertBodyPhoto(OWNER_ID, reviewCount = 10, totalReviewScore = 40.0)
        insertBodyPhoto(FEMALE_PEER_ID, reviewCount = 10, totalReviewScore = 50.0)
        insertBodyPhoto(FEMALE_PEER_ID, reviewCount = 10, totalReviewScore = 30.0)
        insertBodyPhoto(FEMALE_PEER_ID, reviewCount = 1, totalReviewScore = 5.0)
        insertBodyPhoto(FEMALE_PEER_ID, reviewCount = 10, totalReviewScore = 50.0, createdAt = LocalDateTime.now().minusDays(31))
        insertBodyPhoto(MALE_PEER_ID, reviewCount = 10, totalReviewScore = 50.0)

        // when
        val result = mockMvc.perform(
            get("/v1/body/photo/$target").header(AUTHORIZATION, bearerToken(OWNER_ID)),
        )

        // then
        result.andExpect(status().isOk)
            .andExpect(jsonPath("$.data.genderTopRate").value(67.0))
    }

    @Test
    @DisplayName("30일이 지난 사진을 조회해도 상위 비율은 100%를 넘지 않는다")
    fun oldPhotoRateNeverExceedsHundred() {
        // given
        val target = insertBodyPhoto(OWNER_ID, reviewCount = 10, totalReviewScore = 30.0, createdAt = LocalDateTime.now().minusDays(31))
        repeat(4) { insertBodyPhoto(FEMALE_PEER_ID, reviewCount = 10, totalReviewScore = 50.0) }

        // when
        val result = mockMvc.perform(
            get("/v1/body/photo/$target").header(AUTHORIZATION, bearerToken(OWNER_ID)),
        )

        // then
        result.andExpect(status().isOk)
            .andExpect(jsonPath("$.data.genderTopRate").value(100.0))
    }

    @Test
    @DisplayName("리뷰가 완료되지 않은 사진을 조회하면 그 사진도 전체 수에 포함해 상위 비율을 계산한다")
    fun incompletePhotoIsCountedInTotal() {
        // given
        val target = insertBodyPhoto(OWNER_ID, reviewCount = 2, totalReviewScore = 8.0)
        insertBodyPhoto(FEMALE_PEER_ID, reviewCount = 10, totalReviewScore = 50.0)
        insertBodyPhoto(FEMALE_PEER_ID, reviewCount = 10, totalReviewScore = 30.0)

        // when
        val result = mockMvc.perform(
            get("/v1/body/photo/$target").header(AUTHORIZATION, bearerToken(OWNER_ID)),
        )

        // then
        result.andExpect(status().isOk)
            .andExpect(jsonPath("$.data.genderTopRate").value(67.0))
    }

    @Test
    @DisplayName("평균 점수가 같은 사진은 더 높은 순위로 세지 않는다")
    fun equalAverageIsNotHigher() {
        // given
        val target = insertBodyPhoto(OWNER_ID, reviewCount = 10, totalReviewScore = 40.0)
        insertBodyPhoto(FEMALE_PEER_ID, reviewCount = 10, totalReviewScore = 40.0)
        insertBodyPhoto(FEMALE_PEER_ID, reviewCount = 10, totalReviewScore = 30.0)

        // when
        val result = mockMvc.perform(
            get("/v1/body/photo/$target").header(AUTHORIZATION, bearerToken(OWNER_ID)),
        )

        // then
        result.andExpect(status().isOk)
            .andExpect(jsonPath("$.data.genderTopRate").value(33.0))
    }

    companion object {
        private const val OWNER_ID = 900_011L
        private const val OTHER_ID = 900_012L
        private const val MALE_PEER_ID = 900_013L
        private const val FEMALE_PEER_ID = 900_014L
    }

}
