package app.thunder.api.review

import app.thunder.api.photo.BodyPhotoWebTest
import app.thunder.core.notification.client.NotificationClient
import java.time.LocalDateTime
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.mockito.Mockito.mockingDetails
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post

abstract class BodyReviewWebTest : BodyPhotoWebTest() {

    @MockitoBean
    protected lateinit var notificationClient: NotificationClient

    @Autowired
    @Qualifier("applicationTaskExecutor")
    private lateinit var asyncExecutor: ThreadPoolTaskExecutor

    @AfterEach
    fun deleteReviewFixtures() {
        awaitAsyncIdle()
        listOf("dummy_deck", "member_block_relation", "member_fcm_token")
            .forEach { jdbcTemplate.update("DELETE FROM $it") }
    }

    protected fun awaitAsyncIdle() {
        val deadline = System.nanoTime() + ASYNC_TIMEOUT_NANOS
        var idleObservations = 0
        while (idleObservations < IDLE_OBSERVATIONS_REQUIRED) {
            assertThat(System.nanoTime()).describedAs("async tasks did not finish in time").isLessThan(deadline)
            val idle = asyncExecutor.activeCount == 0 && asyncExecutor.threadPoolExecutor.queue.isEmpty()
            idleObservations = if (idle) idleObservations + 1 else 0
            Thread.sleep(POLL_MILLIS)
        }
    }

    protected fun insertQueue(
        memberId: Long,
        bodyPhotoId: Long,
        bodyPhotoMemberId: Long,
        createdAt: LocalDateTime = LocalDateTime.now(),
    ) {
        jdbcTemplate.update(
            """
            INSERT INTO reviewable_body_photo (member_id, body_photo_id, body_photo_member_id, created_at)
            VALUES (?, ?, ?, ?)
            """.trimIndent(),
            memberId, bodyPhotoId, bodyPhotoMemberId, createdAt,
        )
    }

    protected fun insertBlock(memberId: Long, blockedMemberId: Long) {
        jdbcTemplate.update(
            """
            INSERT INTO member_block_relation (member_id, blocked_member_id, created_at, created_by)
            VALUES (?, ?, ?, ?)
            """.trimIndent(),
            memberId, blockedMemberId, LocalDateTime.now(), memberId,
        )
    }

    protected fun insertFcmToken(memberId: Long) {
        jdbcTemplate.update(
            "INSERT INTO member_fcm_token (member_id, fcm_token, created_at) VALUES (?, ?, ?)",
            memberId, "token-$memberId", LocalDateTime.now(),
        )
    }

    protected fun disableReviewCompleteNotification(memberId: Long) {
        jdbcTemplate.update(
            "INSERT INTO member_setting (member_id, settings, created_at) VALUES (?, ?::jsonb, ?)",
            memberId,
            """{"reviewCompleteNotify":false,"reviewRequestNotify":true,"marketingAgreement":false}""",
            LocalDateTime.now(),
        )
    }

    protected fun notificationCount(): Int =
        mockingDetails(notificationClient).invocations.count { it.method.name == "sendNotification" }

    protected fun reviewRequest(memberId: Long, bodyPhotoId: Long, score: Int = 4) =
        post("/v1/body/review")
            .header(AUTHORIZATION, bearerToken(memberId))
            .contentType(APPLICATION_JSON)
            .content("""{"bodyPhotoId":$bodyPhotoId,"score":$score}""")

    protected fun reviewCount(bodyPhotoId: Long): Int =
        jdbcTemplate.queryForObject("SELECT review_count FROM body_photo WHERE body_photo_id = ?", Int::class.java, bodyPhotoId)!!

    protected fun totalReviewScore(bodyPhotoId: Long): Double =
        jdbcTemplate.queryForObject(
            "SELECT total_review_score FROM body_photo WHERE body_photo_id = ?", Double::class.java, bodyPhotoId,
        )!!

    companion object {
        private const val ASYNC_TIMEOUT_NANOS = 15_000_000_000L
        private const val IDLE_OBSERVATIONS_REQUIRED = 3
        private const val POLL_MILLIS = 20L
    }

}
