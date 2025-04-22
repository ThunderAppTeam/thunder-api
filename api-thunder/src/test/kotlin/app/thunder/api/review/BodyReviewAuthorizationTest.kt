package app.thunder.api.review

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class BodyReviewAuthorizationTest : BodyReviewWebTest() {

    @BeforeEach
    fun createMembers() {
        insertMember(OWNER_ID)
        insertMember(REVIEWER_ID)
    }

    @Test
    @DisplayName("큐에 있는 사진을 리뷰하면 기록하고 점수를 반영하며 큐에서 지운다")
    fun reviewQueuedPhoto() {
        // given
        val bodyPhotoId = insertBodyPhoto(OWNER_ID)
        insertQueue(REVIEWER_ID, bodyPhotoId, OWNER_ID)

        // when
        val result = mockMvc.perform(reviewRequest(REVIEWER_ID, bodyPhotoId, 4))

        // then
        result.andExpect(status().isOk)
        assertThat(count("body_review", "body_photo_id = ? AND member_id = ?", bodyPhotoId, REVIEWER_ID)).isEqualTo(1)
        assertThat(reviewCount(bodyPhotoId)).isEqualTo(1)
        assertThat(totalReviewScore(bodyPhotoId)).isEqualTo(4.0)
        assertThat(count("reviewable_body_photo", "member_id = ? AND body_photo_id = ?", REVIEWER_ID, bodyPhotoId))
            .isEqualTo(0)
    }

    @Test
    @DisplayName("한 번도 배정받지 않은 사진을 리뷰하면 403 NOT_REVIEWABLE_BODY_PHOTO로 응답하고 기록하지 않는다")
    fun reviewNeverAssignedPhoto() {
        // given
        val bodyPhotoId = insertBodyPhoto(OWNER_ID)

        // when
        val result = mockMvc.perform(reviewRequest(REVIEWER_ID, bodyPhotoId))

        // then
        assertRejectedWithoutRecord(result, bodyPhotoId)
    }

    @Test
    @DisplayName("자기 사진을 리뷰하면 403 NOT_REVIEWABLE_BODY_PHOTO로 응답하고 기록하지 않는다")
    fun reviewOwnPhoto() {
        // given
        val bodyPhotoId = insertBodyPhoto(REVIEWER_ID)

        // when
        val result = mockMvc.perform(reviewRequest(REVIEWER_ID, bodyPhotoId))

        // then
        assertRejectedWithoutRecord(result, bodyPhotoId)
    }

    @Test
    @DisplayName("차단 관계인 회원의 사진을 리뷰하면 403 NOT_REVIEWABLE_BODY_PHOTO로 응답하고 기록하지 않는다")
    fun reviewBlockedMemberPhoto() {
        // given
        val bodyPhotoId = insertBodyPhoto(OWNER_ID)
        insertBlock(REVIEWER_ID, OWNER_ID)

        // when
        val result = mockMvc.perform(reviewRequest(REVIEWER_ID, bodyPhotoId))

        // then
        assertRejectedWithoutRecord(result, bodyPhotoId)
    }

    @Test
    @DisplayName("신고한 사진을 리뷰하면 403 NOT_REVIEWABLE_BODY_PHOTO로 응답하고 기록하지 않는다")
    fun reviewFlaggedPhoto() {
        // given
        val bodyPhotoId = insertBodyPhoto(OWNER_ID)
        insertFlagHistory(bodyPhotoId, REVIEWER_ID)

        // when
        val result = mockMvc.perform(reviewRequest(REVIEWER_ID, bodyPhotoId))

        // then
        assertRejectedWithoutRecord(result, bodyPhotoId)
    }

    @Test
    @DisplayName("이미 리뷰한 사진을 다시 리뷰하면 409 ALREADY_REVIEWED로 응답한다")
    fun reviewTwice() {
        // given
        val bodyPhotoId = insertBodyPhoto(OWNER_ID)
        insertQueue(REVIEWER_ID, bodyPhotoId, OWNER_ID)
        mockMvc.perform(reviewRequest(REVIEWER_ID, bodyPhotoId)).andExpect(status().isOk)

        // when
        val result = mockMvc.perform(reviewRequest(REVIEWER_ID, bodyPhotoId))

        // then
        result.andExpect(status().isConflict)
            .andExpect(jsonPath("$.errorCode").value("ALREADY_REVIEWED"))
        assertThat(reviewCount(bodyPhotoId)).isEqualTo(1)
    }

    @Test
    @DisplayName("리뷰가 이미 끝난 사진이 큐에 남아 있어도 리뷰하면 409 ALREADY_REVIEW_COMPLETED로 응답하고 기록하지 않는다")
    fun reviewCompletedPhotoStillQueued() {
        // given
        val bodyPhotoId = insertBodyPhoto(OWNER_ID, reviewCount = 10, totalReviewScore = 40.0)
        insertQueue(REVIEWER_ID, bodyPhotoId, OWNER_ID)

        // when
        val result = mockMvc.perform(reviewRequest(REVIEWER_ID, bodyPhotoId))

        // then
        result.andExpect(status().isConflict)
            .andExpect(jsonPath("$.errorCode").value("ALREADY_REVIEW_COMPLETED"))
        assertThat(reviewCount(bodyPhotoId)).isEqualTo(10)
        assertThat(count("body_review", "body_photo_id = ?", bodyPhotoId)).isEqualTo(0)
    }

    @Test
    @DisplayName("점수가 1에서 5 사이가 아니면 400으로 응답하고 기록하지 않는다")
    fun reviewScoreOutOfRange() {
        // given
        val bodyPhotoId = insertBodyPhoto(OWNER_ID)
        insertQueue(REVIEWER_ID, bodyPhotoId, OWNER_ID)

        // when
        val tooLow = mockMvc.perform(reviewRequest(REVIEWER_ID, bodyPhotoId, 0))
        val tooHigh = mockMvc.perform(reviewRequest(REVIEWER_ID, bodyPhotoId, 6))

        // then
        tooLow.andExpect(status().isBadRequest)
        tooHigh.andExpect(status().isBadRequest)
        assertThat(count("body_review", "body_photo_id = ?", bodyPhotoId)).isEqualTo(0)
    }

    @Test
    @DisplayName("10번째 리뷰가 달리면 업로더의 알림 설정과 관계없이 그 사진의 큐를 모두 지운다")
    fun completionClearsQueueRegardlessOfNotificationSetting() {
        // given
        disableReviewCompleteNotification(OWNER_ID)
        val bodyPhotoId = insertBodyPhoto(OWNER_ID, reviewCount = 9, totalReviewScore = 36.0)
        insertQueue(REVIEWER_ID, bodyPhotoId, OWNER_ID)
        insertMember(OTHER_ID)
        insertQueue(OTHER_ID, bodyPhotoId, OWNER_ID)

        // when
        val result = mockMvc.perform(reviewRequest(REVIEWER_ID, bodyPhotoId))

        // then
        result.andExpect(status().isOk)
        assertThat(count("reviewable_body_photo", "body_photo_id = ?", bodyPhotoId)).isEqualTo(0)
        awaitAsyncIdle()
        assertThat(notificationCount()).isEqualTo(0)
    }

    private fun assertRejectedWithoutRecord(result: ResultActions, bodyPhotoId: Long) {
        result.andExpect(status().isForbidden)
            .andExpect(jsonPath("$.errorCode").value("NOT_REVIEWABLE_BODY_PHOTO"))
        assertThat(count("body_review", "body_photo_id = ?", bodyPhotoId)).isEqualTo(0)
        assertThat(reviewCount(bodyPhotoId)).isEqualTo(0)
    }

    companion object {
        private const val OWNER_ID = 900_101L
        private const val REVIEWER_ID = 900_102L
        private const val OTHER_ID = 900_103L
    }

}
