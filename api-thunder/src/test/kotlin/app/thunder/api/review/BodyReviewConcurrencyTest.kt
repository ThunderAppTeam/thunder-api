package app.thunder.api.review

import app.thunder.api.ConcurrentRunner
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class BodyReviewConcurrencyTest : BodyReviewWebTest() {

    @BeforeEach
    fun createOwner() {
        insertMember(OWNER_ID)
        insertFcmToken(OWNER_ID)
    }

    @Test
    @DisplayName("서로 다른 회원 8명이 동시에 리뷰해도 리뷰 수와 점수 합계가 하나도 유실되지 않는다")
    fun concurrentReviewsByDifferentMembers() {
        // given
        val bodyPhotoId = insertBodyPhoto(OWNER_ID)
        val scores = listOf(1, 2, 3, 4, 5, 5, 4, 3)
        val reviewerIds = scores.indices.map { REVIEWER_BASE_ID + it }
        reviewerIds.forEach {
            insertMember(it)
            insertQueue(it, bodyPhotoId, OWNER_ID)
        }

        // when
        val statuses = ConcurrentRunner.runAll(
            reviewerIds.zip(scores).map { (reviewerId, score) ->
                { mockMvc.perform(reviewRequest(reviewerId, bodyPhotoId, score)).andReturn().response.status }
            },
        ).map { it.getOrThrow() }

        // then
        assertThat(statuses).containsOnly(200)
        assertThat(reviewCount(bodyPhotoId)).isEqualTo(8)
        assertThat(totalReviewScore(bodyPhotoId)).isEqualTo(scores.sum().toDouble())
        assertThat(count("body_review", "body_photo_id = ?", bodyPhotoId)).isEqualTo(8)
    }

    @Test
    @DisplayName("리뷰가 9개인 사진에 3명이 동시에 리뷰하면 한 명만 성공하고 나머지는 403 또는 409이며 완료 처리는 한 번만 실행된다")
    fun concurrentReviewsAtCompletionBoundary() {
        // given
        val bodyPhotoId = insertBodyPhoto(OWNER_ID, reviewCount = 9, totalReviewScore = 36.0)
        val reviewerIds = (0 until 3).map { REVIEWER_BASE_ID + it }
        reviewerIds.forEach {
            insertMember(it)
            insertQueue(it, bodyPhotoId, OWNER_ID)
        }

        // when
        val responses = ConcurrentRunner.runAll(
            reviewerIds.map { reviewerId ->
                { mockMvc.perform(reviewRequest(reviewerId, bodyPhotoId, 4)).andReturn().response }
            },
        ).map { it.getOrThrow() }
        awaitAsyncIdle()

        // then
        val statuses = responses.map { it.status }
        assertThat(statuses.count { it == 200 }).isEqualTo(1)
        assertThat(statuses.filter { it != 200 }).hasSize(2).allMatch { it == 403 || it == 409 }
        assertThat(reviewCount(bodyPhotoId)).isEqualTo(10)
        assertThat(totalReviewScore(bodyPhotoId)).isEqualTo(40.0)
        assertThat(count("body_review", "body_photo_id = ?", bodyPhotoId)).isEqualTo(1)
        assertThat(count("reviewable_body_photo", "body_photo_id = ?", bodyPhotoId)).isEqualTo(0)
        assertThat(notificationCount()).isEqualTo(1)
    }

    @Test
    @DisplayName("같은 회원이 같은 사진을 동시에 리뷰하면 리뷰 한 건만 남고 진 쪽은 점수를 반영하지 않는다")
    fun concurrentDuplicateReviewBySameMember() {
        // given
        val bodyPhotoId = insertBodyPhoto(OWNER_ID)
        insertMember(REVIEWER_BASE_ID)
        insertQueue(REVIEWER_BASE_ID, bodyPhotoId, OWNER_ID)

        // when
        val statuses = ConcurrentRunner.runAll(
            List(2) { { mockMvc.perform(reviewRequest(REVIEWER_BASE_ID, bodyPhotoId, 5)).andReturn().response.status } },
        ).map { it.getOrThrow() }

        // then
        assertThat(statuses).containsExactlyInAnyOrder(200, 409)
        assertThat(count("body_review", "body_photo_id = ? AND member_id = ?", bodyPhotoId, REVIEWER_BASE_ID))
            .isEqualTo(1)
        assertThat(reviewCount(bodyPhotoId)).isEqualTo(1)
        assertThat(totalReviewScore(bodyPhotoId)).isEqualTo(5.0)
    }

    companion object {
        private const val OWNER_ID = 900_201L
        private const val REVIEWER_BASE_ID = 900_210L
    }

}
