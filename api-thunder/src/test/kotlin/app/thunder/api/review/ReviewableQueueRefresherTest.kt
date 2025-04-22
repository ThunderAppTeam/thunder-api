package app.thunder.api.review

import app.thunder.api.ConcurrentRunner
import app.thunder.api.event.ReviewableQueueRefresher
import app.thunder.core.flag.repository.FlagHistoryRepository
import app.thunder.core.photo.repository.BodyPhotoRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get

class ReviewableQueueRefresherTest : BodyReviewWebTest() {

    @Autowired
    private lateinit var refresher: ReviewableQueueRefresher

    @Autowired
    private lateinit var bodyPhotoRepository: BodyPhotoRepository

    @Autowired
    private lateinit var flagHistoryRepository: FlagHistoryRepository

    @BeforeEach
    fun createMembers() {
        insertMember(OWNER_ID)
        insertMember(REVIEWER_ID)
    }

    @Test
    @DisplayName("빈 큐에 refresh가 동시에 두 번 실행되어도 큐는 30개를 넘지 않고 중복이 없다")
    fun concurrentRefreshKeepsQueueWithinMaximum() {
        // given
        repeat(PHOTO_POOL_SIZE) { insertBodyPhoto(OWNER_ID) }

        repeat(ROUNDS) {
            jdbcTemplate.update("DELETE FROM reviewable_body_photo")

            // when
            ConcurrentRunner.runAll(List(2) { { refreshAll(REVIEWER_ID) } }).forEach { it.getOrThrow() }

            // then
            val queued = jdbcTemplate.queryForList(
                "SELECT body_photo_id FROM reviewable_body_photo WHERE member_id = ?", Long::class.java, REVIEWER_ID,
            )
            assertThat(queued).hasSize(30).doesNotHaveDuplicates()
        }
    }

    @Test
    @DisplayName("이미 탈퇴한 회원의 refresh는 큐를 만들지 않는다")
    fun refreshSkipsMissingMember() {
        // given
        repeat(5) { insertBodyPhoto(OWNER_ID) }

        // when
        refreshAll(MISSING_MEMBER_ID)

        // then
        assertThat(count("reviewable_body_photo", "member_id = ?", MISSING_MEMBER_ID)).isEqualTo(0)
    }

    @Test
    @DisplayName("리뷰와 refresh가 동시에 실행되어도 방금 리뷰한 사진은 큐에 다시 들어가지 않는다")
    fun reviewAndRefreshRace() {
        // given
        repeat(PHOTO_POOL_SIZE) { insertBodyPhoto(OWNER_ID) }

        repeat(ROUNDS) {
            val reviewed = insertBodyPhoto(OWNER_ID)
            jdbcTemplate.update("DELETE FROM reviewable_body_photo")
            insertQueue(REVIEWER_ID, reviewed, OWNER_ID)

            // when
            ConcurrentRunner.runAll(
                listOf(
                    { mockMvc.perform(reviewRequest(REVIEWER_ID, reviewed)).andReturn().response.status },
                    { refreshAll(REVIEWER_ID); 0 },
                ),
            ).forEach { it.getOrThrow() }
            awaitAsyncIdle()

            // then
            assertThat(count("body_review", "body_photo_id = ? AND member_id = ?", reviewed, REVIEWER_ID)).isEqualTo(1)
            assertThat(count("reviewable_body_photo", "member_id = ? AND body_photo_id = ?", REVIEWER_ID, reviewed))
                .isEqualTo(0)
            jdbcTemplate.update("DELETE FROM body_review")
        }
    }

    @Test
    @DisplayName("큐 행을 모두 지운 회원이 GET /v1/body/review를 호출하면 큐가 다시 채워진다")
    fun getReviewRefillsClearedQueue() {
        // given
        repeat(PHOTO_POOL_SIZE) { insertBodyPhoto(OWNER_ID) }
        jdbcTemplate.update("DELETE FROM reviewable_body_photo")

        // when
        mockMvc.perform(get("/v1/body/review").param("size", "5").header(AUTHORIZATION, bearerToken(REVIEWER_ID)))
        awaitAsyncIdle()

        // then
        assertThat(count("reviewable_body_photo", "member_id = ?", REVIEWER_ID)).isEqualTo(30)
    }

    @Test
    @DisplayName("큐에 완료된 사진 행이 11개 있어도 refresh 한 번이면 유효한 사진으로 교체된다")
    fun refreshReplacesCompletedPhotoRows() {
        // given
        val completedIds = List(11) { insertBodyPhoto(OWNER_ID, reviewCount = 10, totalReviewScore = 40.0) }
        completedIds.forEach { insertQueue(REVIEWER_ID, it, OWNER_ID) }
        val validIds = List(PHOTO_POOL_SIZE) { insertBodyPhoto(OWNER_ID) }

        // when
        refreshAll(REVIEWER_ID)

        // then
        val queued = queuedPhotoIds()
        assertThat(queued).hasSize(30).doesNotHaveDuplicates()
        assertThat(queued).doesNotContainAnyElementsOf(completedIds)
        assertThat(validIds).containsAll(queued)
    }

    @Test
    @DisplayName("refresh는 없어졌거나 내 사진이거나 이미 리뷰와 신고를 했거나 신고가 3개 이상이거나 차단한 회원의 사진 행을 큐에서 지운다")
    fun refreshRemovesStaleRows() {
        // given
        insertMember(BLOCKED_ID)
        val staleIds = listOf(
            MISSING_PHOTO_ID,
            insertBodyPhoto(REVIEWER_ID),
            insertBodyPhoto(OWNER_ID).also { insertBodyReview(it, REVIEWER_ID, 4) },
            insertBodyPhoto(OWNER_ID).also { insertFlagHistory(it, REVIEWER_ID) },
            insertBodyPhoto(OWNER_ID).also { photoId -> (1L..3L).forEach { insertFlagHistory(photoId, OWNER_ID + 100 + it) } },
            insertBodyPhoto(BLOCKED_ID),
        )
        staleIds.forEach { insertQueue(REVIEWER_ID, it, OWNER_ID) }
        insertBlock(REVIEWER_ID, BLOCKED_ID)
        val keptId = insertBodyPhoto(OWNER_ID)
        insertQueue(REVIEWER_ID, keptId, OWNER_ID)

        // when
        refreshAll(REVIEWER_ID)

        // then
        val queued = queuedPhotoIds()
        assertThat(queued).doesNotContainAnyElementsOf(staleIds)
        assertThat(queued).contains(keptId)
    }

    @Test
    @DisplayName("스냅샷 이후 완료된 사진은 큐에 넣지 않는다")
    fun refreshSkipsPhotoCompletedAfterSnapshot() {
        // given
        val completedAfterSnapshot = insertBodyPhoto(OWNER_ID)
        val snapshot = bodyPhotoRepository.getNotReviewCompletedAll()
        jdbcTemplate.update("UPDATE body_photo SET review_count = 10 WHERE body_photo_id = ?", completedAfterSnapshot)
        val deletedAfterSnapshot = insertBodyPhoto(OWNER_ID)
        val snapshotWithDeleted = bodyPhotoRepository.getNotReviewCompletedAll()
        jdbcTemplate.update("DELETE FROM body_photo WHERE body_photo_id = ?", deletedAfterSnapshot)

        // when
        refresher.refresh(REVIEWER_ID, snapshot + snapshotWithDeleted, emptyMap())

        // then
        assertThat(queuedPhotoIds()).doesNotContain(completedAfterSnapshot, deletedAfterSnapshot)
    }

    private fun queuedPhotoIds(): List<Long> = jdbcTemplate.queryForList(
        "SELECT body_photo_id FROM reviewable_body_photo WHERE member_id = ?", Long::class.java, REVIEWER_ID,
    )

    private fun refreshAll(memberId: Long) {
        refresher.refresh(
            memberId,
            bodyPhotoRepository.getNotReviewCompletedAll(),
            flagHistoryRepository.getFlagCountByBodyPhotoId(),
        )
    }

    companion object {
        private const val OWNER_ID = 900_401L
        private const val REVIEWER_ID = 900_402L
        private const val MISSING_MEMBER_ID = 900_499L
        private const val BLOCKED_ID = 900_403L
        private const val MISSING_PHOTO_ID = 987_654_321L
        private const val PHOTO_POOL_SIZE = 80
        private const val ROUNDS = 5
    }

}
