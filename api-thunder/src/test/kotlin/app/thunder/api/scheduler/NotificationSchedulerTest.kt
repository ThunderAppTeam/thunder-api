package app.thunder.api.scheduler

import app.thunder.api.photo.BodyPhotoWebTest
import app.thunder.core.member.repository.FcmTokenRepository
import app.thunder.core.member.repository.MemberRepository
import app.thunder.core.notification.client.NotificationClient
import app.thunder.core.photo.repository.BodyPhotoRepository
import app.thunder.core.review.repository.BodyReviewRepository
import app.thunder.core.review.repository.ReviewableBodyPhotoRepository
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.MessagingErrorCode
import java.time.LocalDateTime
import org.assertj.core.api.Assertions.assertThat
import org.instancio.Instancio
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.BDDMockito.willAnswer
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockingDetails
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.util.ReflectionTestUtils

class NotificationSchedulerTest : BodyPhotoWebTest() {

    @Autowired
    private lateinit var memberRepository: MemberRepository

    @Autowired
    private lateinit var bodyPhotoRepository: BodyPhotoRepository

    @Autowired
    private lateinit var bodyReviewRepository: BodyReviewRepository

    @Autowired
    private lateinit var reviewableBodyPhotoRepository: ReviewableBodyPhotoRepository

    @Autowired
    private lateinit var fcmTokenRepository: FcmTokenRepository

    @Autowired
    private lateinit var notificationClient: NotificationClient

    private lateinit var notificationScheduler: NotificationScheduler

    @BeforeEach
    fun createScheduler() {
        notificationScheduler = NotificationScheduler(
            memberRepository,
            bodyPhotoRepository,
            bodyReviewRepository,
            reviewableBodyPhotoRepository,
            fcmTokenRepository,
            notificationClient,
        )
    }

    @BeforeEach
    fun createMembers() {
        insertMember(PHOTO_OWNER_ID)
    }

    @AfterEach
    fun deleteSchedulerFixtures() {
        jdbcTemplate.update("DELETE FROM member_fcm_token")
    }

    @Test
    @DisplayName("테스터에게 큐의 첫 사진 이미지와 함께 리뷰 요청 알림을 보낸다")
    fun notifiesTesterWithQueuedPhotoImage() {
        // given
        val token = Instancio.create(String::class.java)
        val imageUrl = "https://test-bucket.s3.ap-northeast-2.amazonaws.com/$PHOTO_OWNER_ID/body_photo/queued.jpg"
        val bodyPhotoId = insertBodyPhoto(PHOTO_OWNER_ID, imageUrl = imageUrl)
        prepareMember(TESTER_ID, token)
        insertQueue(TESTER_ID, bodyPhotoId)

        // when
        notificationScheduler.notifyReviewableForTest()

        // then
        val message = sentMessages().single()
        assertThat(field(message, "token")).isEqualTo(token)
        val notification = field(message, "notification")
        assertThat(field(notification, "title")).isEqualTo("새로운 눈바디 평가 요청 👀")
        assertThat(field(notification, "body")).isEqualTo("누군가 user${TESTER_ID}님께 눈바디 평가를 요청했어요.")
        assertThat(field(notification, "image")).isEqualTo(imageUrl)
        assertThat((field(message, "data") as Map<*, *>)["routePath"]).isEqualTo("/")
    }

    @Test
    @DisplayName("테스터가 아닌 회원에게는 알림을 보내지 않는다")
    fun doesNotNotifyNonTester() {
        // given
        insertMember(NON_TESTER_ID)
        prepareMember(NON_TESTER_ID, Instancio.create(String::class.java))
        insertQueue(NON_TESTER_ID, insertBodyPhoto(PHOTO_OWNER_ID))

        // when
        notificationScheduler.notifyReviewableForTest()

        // then
        assertThat(sentMessages()).isEmpty()
    }

    @Test
    @DisplayName("최근 30분 안에 리뷰한 테스터는 제외하고 그보다 오래전에 리뷰한 테스터에게만 알림을 보낸다")
    fun notifiesOnlyTestersWithoutRecentReview() {
        // given
        val recentToken = Instancio.create(String::class.java)
        val staleToken = Instancio.create(String::class.java)
        val bodyPhotoId = insertBodyPhoto(PHOTO_OWNER_ID)
        prepareMember(TESTER_ID, recentToken)
        prepareMember(OTHER_TESTER_ID, staleToken)
        insertQueue(TESTER_ID, bodyPhotoId)
        insertQueue(OTHER_TESTER_ID, bodyPhotoId)
        insertReviewAt(bodyPhotoId, TESTER_ID, LocalDateTime.now().minusMinutes(10))
        insertReviewAt(bodyPhotoId, OTHER_TESTER_ID, LocalDateTime.now().minusMinutes(90))

        // when
        notificationScheduler.notifyReviewableForTest()

        // then
        assertThat(sentMessages().map { field(it, "token") }).containsExactly(staleToken)
    }

    @Test
    @DisplayName("리뷰 요청 알림을 끈 테스터에게는 알림을 보내지 않는다")
    fun doesNotNotifyTesterWhoDisabledReviewRequest() {
        // given
        prepareMember(TESTER_ID, Instancio.create(String::class.java), reviewRequestNotify = false)
        insertQueue(TESTER_ID, insertBodyPhoto(PHOTO_OWNER_ID))

        // when
        notificationScheduler.notifyReviewableForTest()

        // then
        assertThat(sentMessages()).isEmpty()
    }

    @Test
    @DisplayName("큐에 사진이 없는 테스터에게는 알림을 보내지 않는다")
    fun doesNotNotifyTesterWithEmptyQueue() {
        // given
        prepareMember(TESTER_ID, Instancio.create(String::class.java))

        // when
        notificationScheduler.notifyReviewableForTest()

        // then
        assertThat(sentMessages()).isEmpty()
    }

    @Test
    @DisplayName("FCM 토큰이 없는 테스터에게는 알림을 보내지 않는다")
    fun doesNotNotifyTesterWithoutToken() {
        // given
        insertMember(TESTER_ID)
        insertSettings(TESTER_ID, reviewRequestNotify = true)
        insertQueue(TESTER_ID, insertBodyPhoto(PHOTO_OWNER_ID))

        // when
        notificationScheduler.notifyReviewableForTest()

        // then
        assertThat(sentMessages()).isEmpty()
    }

    @Test
    @DisplayName("큐 항목의 사진 행이 사라졌어도 이미지 없이 알림을 보낸다")
    fun notifiesWithoutImageWhenPhotoRowIsMissing() {
        // given
        val token = Instancio.create(String::class.java)
        prepareMember(TESTER_ID, token)
        insertQueue(TESTER_ID, MISSING_BODY_PHOTO_ID)

        // when
        notificationScheduler.notifyReviewableForTest()

        // then
        val message = sentMessages().single()
        assertThat(field(message, "token")).isEqualTo(token)
        assertThat(field(field(message, "notification"), "image")).isNull()
    }

    @Test
    @DisplayName("FCM이 토큰을 무효로 거절하면 그 테스터의 토큰만 지운다")
    fun deletesOnlyRejectedTokens() {
        // given
        val invalidToken = Instancio.create(String::class.java)
        val validToken = Instancio.create(String::class.java)
        val bodyPhotoId = insertBodyPhoto(PHOTO_OWNER_ID)
        prepareMember(TESTER_ID, invalidToken)
        prepareMember(OTHER_TESTER_ID, validToken)
        insertQueue(TESTER_ID, bodyPhotoId)
        insertQueue(OTHER_TESTER_ID, bodyPhotoId)
        rejectToken(invalidToken, MessagingErrorCode.UNREGISTERED)

        // when
        notificationScheduler.notifyReviewableForTest()

        // then
        assertThat(sentMessages()).hasSize(2)
        assertThat(tokensOf(TESTER_ID)).isEmpty()
        assertThat(tokensOf(OTHER_TESTER_ID)).containsExactly(validToken)
    }

    @Test
    @DisplayName("FCM 전송이 일시 오류로 실패하면 토큰을 지우지 않는다")
    fun keepsTokenOnTransientFailure() {
        // given
        val token = Instancio.create(String::class.java)
        prepareMember(TESTER_ID, token)
        insertQueue(TESTER_ID, insertBodyPhoto(PHOTO_OWNER_ID))
        rejectToken(token, MessagingErrorCode.UNAVAILABLE)

        // when
        notificationScheduler.notifyReviewableForTest()

        // then
        assertThat(sentMessages()).hasSize(1)
        assertThat(tokensOf(TESTER_ID)).containsExactly(token)
    }

    @Test
    @DisplayName("매일 저녁 알림은 테스터를 제외한 회원에게만 큐의 첫 사진 이미지와 함께 보낸다")
    fun dailyNotificationSkipsTesters() {
        // given
        val token = Instancio.create(String::class.java)
        val imageUrl = "https://test-bucket.s3.ap-northeast-2.amazonaws.com/$PHOTO_OWNER_ID/body_photo/daily.jpg"
        val bodyPhotoId = insertBodyPhoto(PHOTO_OWNER_ID, imageUrl = imageUrl)
        prepareMember(TESTER_ID, Instancio.create(String::class.java))
        prepareMember(NON_TESTER_ID, token)
        insertQueue(TESTER_ID, bodyPhotoId)
        insertQueue(NON_TESTER_ID, bodyPhotoId)

        // when
        notificationScheduler.notifyReviewable()

        // then
        val message = sentMessages().single()
        assertThat(field(message, "token")).isEqualTo(token)
        assertThat(field(field(message, "notification"), "image")).isEqualTo(imageUrl)
    }

    @Test
    @DisplayName("매일 저녁 알림은 오늘 이미 리뷰한 회원을 제외하고 이전 날에 마지막으로 리뷰한 회원에게는 보낸다")
    fun dailyNotificationSkipsMembersWhoReviewedToday() {
        // given
        val reviewedTodayToken = Instancio.create(String::class.java)
        val reviewedEarlierToken = Instancio.create(String::class.java)
        val bodyPhotoId = insertBodyPhoto(PHOTO_OWNER_ID)
        prepareMember(NON_TESTER_ID, reviewedTodayToken)
        prepareMember(OTHER_NON_TESTER_ID, reviewedEarlierToken)
        insertQueue(NON_TESTER_ID, bodyPhotoId)
        insertQueue(OTHER_NON_TESTER_ID, bodyPhotoId)
        insertReviewAt(bodyPhotoId, NON_TESTER_ID, LocalDateTime.now())
        insertReviewAt(bodyPhotoId, OTHER_NON_TESTER_ID, LocalDateTime.now().minusDays(3))

        // when
        notificationScheduler.notifyReviewable()

        // then
        assertThat(sentMessages().map { field(it, "token") }).containsExactly(reviewedEarlierToken)
    }

    @Test
    @DisplayName("매일 저녁 알림은 큐 항목의 사진 행이 사라졌어도 이미지 없이 보낸다")
    fun dailyNotificationWithoutImageWhenPhotoRowIsMissing() {
        // given
        val token = Instancio.create(String::class.java)
        prepareMember(NON_TESTER_ID, token)
        insertQueue(NON_TESTER_ID, MISSING_BODY_PHOTO_ID)

        // when
        notificationScheduler.notifyReviewable()

        // then
        val message = sentMessages().single()
        assertThat(field(message, "token")).isEqualTo(token)
        assertThat(field(field(message, "notification"), "image")).isNull()
    }

    @Test
    @DisplayName("매일 저녁 알림은 FCM 토큰이 없는 회원에게 보내지 않는다")
    fun dailyNotificationSkipsMemberWithoutToken() {
        // given
        insertMember(NON_TESTER_ID)
        insertSettings(NON_TESTER_ID, reviewRequestNotify = true)
        insertQueue(NON_TESTER_ID, insertBodyPhoto(PHOTO_OWNER_ID))

        // when
        notificationScheduler.notifyReviewable()

        // then
        assertThat(sentMessages()).isEmpty()
    }

    private fun prepareMember(memberId: Long, token: String, reviewRequestNotify: Boolean = true) {
        if (jdbcTemplate.queryForObject("SELECT COUNT(*) FROM member WHERE member_id = ?", Int::class.java, memberId) == 0) {
            insertMember(memberId)
        }
        insertSettings(memberId, reviewRequestNotify)
        jdbcTemplate.update(
            "INSERT INTO member_fcm_token (member_id, fcm_token, created_at) VALUES (?, ?, ?)",
            memberId, token, LocalDateTime.now(),
        )
    }

    private fun insertSettings(memberId: Long, reviewRequestNotify: Boolean) {
        jdbcTemplate.update(
            "INSERT INTO member_setting (member_id, settings, created_at) VALUES (?, ?::jsonb, ?)",
            memberId,
            """{"reviewCompleteNotify":true,"reviewRequestNotify":$reviewRequestNotify,"marketingAgreement":false}""",
            LocalDateTime.now(),
        )
    }

    private fun insertQueue(memberId: Long, bodyPhotoId: Long) {
        jdbcTemplate.update(
            """
            INSERT INTO reviewable_body_photo (member_id, body_photo_id, body_photo_member_id, created_at)
            VALUES (?, ?, ?, ?)
            """.trimIndent(),
            memberId, bodyPhotoId, PHOTO_OWNER_ID, LocalDateTime.now(),
        )
    }

    private fun insertReviewAt(bodyPhotoId: Long, memberId: Long, createdAt: LocalDateTime) {
        jdbcTemplate.update(
            "INSERT INTO body_review (body_photo_id, member_id, score, created_at) VALUES (?, ?, ?, ?)",
            bodyPhotoId, memberId, 4, createdAt,
        )
    }

    private fun tokensOf(memberId: Long): List<String> = jdbcTemplate.queryForList(
        "SELECT fcm_token FROM member_fcm_token WHERE member_id = ?", String::class.java, memberId,
    )

    private fun rejectToken(rejectedToken: String, errorCode: MessagingErrorCode) {
        val exception = mock(FirebaseMessagingException::class.java)
        given(exception.messagingErrorCode).willReturn(errorCode)
        willAnswer { invocation ->
            if (field(invocation.getArgument<Message>(0), "token") == rejectedToken) throw exception
            "message-id"
        }.given(firebaseMessaging).send(anyMessage())
    }

    private fun anyMessage(): Message {
        ArgumentMatchers.any(Message::class.java)
        return Message.builder().setToken("matcher-placeholder").build()
    }

    private fun sentMessages(): List<Message> = mockingDetails(firebaseMessaging).invocations
        .filter { it.method.name == "send" }
        .map { it.arguments[0] as Message }

    private fun field(target: Any?, name: String): Any? = ReflectionTestUtils.getField(target!!, name)

    companion object {
        private const val PHOTO_OWNER_ID = 900_301L
        private const val NON_TESTER_ID = 900_302L
        private const val OTHER_NON_TESTER_ID = 900_303L
        private const val TESTER_ID = 1L
        private const val OTHER_TESTER_ID = 2L
        private const val MISSING_BODY_PHOTO_ID = 987_654L
    }

}
