package app.thunder.api.member

import app.thunder.api.review.BodyReviewWebTest
import app.thunder.core.review.repository.ReviewableBodyPhotoRepository
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate

abstract class MemberWebTest : BodyReviewWebTest() {

    @Autowired
    private lateinit var transactionManager: PlatformTransactionManager

    @Autowired
    private lateinit var reviewableBodyPhotoRepository: ReviewableBodyPhotoRepository

    protected fun insertSettings(
        memberId: Long,
        reviewCompleteNotify: Boolean = true,
        reviewRequestNotify: Boolean = true,
    ) {
        jdbcTemplate.update(
            "INSERT INTO member_setting (member_id, settings, created_at) VALUES (?, ?::jsonb, ?)",
            memberId,
            """{"reviewCompleteNotify":$reviewCompleteNotify,"reviewRequestNotify":$reviewRequestNotify,"marketingAgreement":false}""",
            LocalDateTime.now(),
        )
    }

    protected fun insertToken(memberId: Long, fcmToken: String) {
        jdbcTemplate.update(
            "INSERT INTO member_fcm_token (member_id, fcm_token, created_at) VALUES (?, ?, ?)",
            memberId, fcmToken, LocalDateTime.now(),
        )
    }

    protected fun insertDummyDeck(memberId: Long, bodyPhotoId: Long, bodyPhotoMemberId: Long) {
        jdbcTemplate.update(
            """
            INSERT INTO dummy_deck (member_id, body_photo_id, body_photo_member_id, nickname, age, created_at)
            VALUES (?, ?, ?, 'dummy', 25, ?)
            """.trimIndent(),
            memberId, bodyPhotoId, bodyPhotoMemberId, LocalDateTime.now(),
        )
    }

    protected fun tokenOf(memberId: Long): String? = jdbcTemplate.queryForList(
        "SELECT fcm_token FROM member_fcm_token WHERE member_id = ?", String::class.java, memberId,
    ).firstOrNull()

    protected fun blockRequest(requesterId: Long, blockedMemberId: Long): MockHttpServletRequestBuilder =
        post("/v1/member/block")
            .header(AUTHORIZATION, bearerToken(requesterId))
            .contentType(APPLICATION_JSON)
            .content("""{"blockedMemberId":$blockedMemberId}""")

    protected fun deletionRequest(memberId: Long): MockHttpServletRequestBuilder =
        post("/v1/member/deletion")
            .header(AUTHORIZATION, bearerToken(memberId))
            .contentType(APPLICATION_JSON)
            .content("""{"deletionReason":"OTHER","otherReason":"test"}""")

    protected fun logoutRequest(memberId: Long): MockHttpServletRequestBuilder =
        put("/v1/member/logout").header(AUTHORIZATION, bearerToken(memberId))

    protected fun fcmTokenRequest(memberId: Long, fcmToken: String): MockHttpServletRequestBuilder =
        post("/v1/member/fcm-token")
            .header(AUTHORIZATION, bearerToken(memberId))
            .contentType(APPLICATION_JSON)
            .content("""{"fcmToken":"$fcmToken"}""")

    protected fun whileHoldingQueueLock(memberId: Long, action: () -> Unit) {
        val held = CountDownLatch(1)
        val release = CountDownLatch(1)
        val holder = CompletableFuture.runAsync {
            TransactionTemplate(transactionManager).executeWithoutResult {
                reviewableBodyPhotoRepository.lockQueue(memberId)
                held.countDown()
                release.await(LOCK_HOLD_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            }
        }
        try {
            check(held.await(LOCK_HOLD_TIMEOUT_SECONDS, TimeUnit.SECONDS)) { "lock holder did not start" }
            action()
        } finally {
            release.countDown()
            holder.get(LOCK_HOLD_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        }
    }

    companion object {
        private const val LOCK_HOLD_TIMEOUT_SECONDS = 15L
        const val BLOCKED_WAIT_MILLIS = 700L
    }

}
