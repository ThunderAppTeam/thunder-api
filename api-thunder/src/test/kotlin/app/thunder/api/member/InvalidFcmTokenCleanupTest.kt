package app.thunder.api.member

import app.thunder.api.scheduler.NotificationScheduler
import app.thunder.core.member.repository.FcmTokenRepository
import app.thunder.core.notification.domain.NotificationResult
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.BDDMockito.willAnswer
import org.springframework.beans.factory.annotation.Autowired

class InvalidFcmTokenCleanupTest : MemberWebTest() {

    @Autowired
    private lateinit var notificationScheduler: NotificationScheduler

    @Autowired
    private lateinit var fcmTokenRepository: FcmTokenRepository

    @BeforeEach
    fun createMembers() {
        insertMember(OWNER_ID)
        insertMember(REVIEWER_ID)
    }

    @Test
    @DisplayName("리뷰 완료 알림이 무효 토큰으로 거절되면 그 회원의 FCM 토큰을 지운다")
    fun reviewCompleteNotificationDeletesInvalidToken() {
        // given
        insertToken(OWNER_ID, OWNER_TOKEN)
        respondWith(NotificationResult.INVALID_TOKEN)
        val bodyPhotoId = insertBodyPhoto(OWNER_ID, reviewCount = 9, totalReviewScore = 36.0)
        insertQueue(REVIEWER_ID, bodyPhotoId, OWNER_ID)

        // when
        mockMvc.perform(reviewRequest(REVIEWER_ID, bodyPhotoId))
        awaitAsyncIdle()

        // then
        assertThat(notificationCount()).isEqualTo(1)
        assertThat(tokenOf(OWNER_ID)).isNull()
    }

    @Test
    @DisplayName("리뷰 완료 알림이 일시 오류로 실패하면 FCM 토큰을 지우지 않는다")
    fun reviewCompleteNotificationKeepsTokenOnFailure() {
        // given
        insertToken(OWNER_ID, OWNER_TOKEN)
        respondWith(NotificationResult.FAILED)
        val bodyPhotoId = insertBodyPhoto(OWNER_ID, reviewCount = 9, totalReviewScore = 36.0)
        insertQueue(REVIEWER_ID, bodyPhotoId, OWNER_ID)

        // when
        mockMvc.perform(reviewRequest(REVIEWER_ID, bodyPhotoId))
        awaitAsyncIdle()

        // then
        assertThat(notificationCount()).isEqualTo(1)
        assertThat(tokenOf(OWNER_ID)).isEqualTo(OWNER_TOKEN)
    }

    @Test
    @DisplayName("리뷰 요청 스케줄러는 무효 토큰으로 거절된 회원의 토큰만 지운다")
    fun schedulerDeletesOnlyInvalidTokens() {
        // given
        listOf(OWNER_ID, REVIEWER_ID).forEach { insertSettings(it) }
        insertToken(OWNER_ID, OWNER_TOKEN)
        insertToken(REVIEWER_ID, REVIEWER_TOKEN)
        val ownerPhoto = insertBodyPhoto(OWNER_ID)
        val reviewerPhoto = insertBodyPhoto(REVIEWER_ID)
        insertQueue(OWNER_ID, reviewerPhoto, REVIEWER_ID)
        insertQueue(REVIEWER_ID, ownerPhoto, OWNER_ID)
        willAnswer { invocation ->
            if (invocation.getArgument<String>(0) == OWNER_TOKEN) NotificationResult.INVALID_TOKEN
            else NotificationResult.SENT
        }.given(notificationClient).sendNotification(anyText(), anyText(), anyText(), anyNullableText(), anyText())

        // when
        notificationScheduler.notifyReviewable()

        // then
        assertThat(notificationCount()).isEqualTo(2)
        assertThat(tokenOf(OWNER_ID)).isNull()
        assertThat(tokenOf(REVIEWER_ID)).isEqualTo(REVIEWER_TOKEN)
    }

    @Test
    @DisplayName("리뷰 완료 알림이 무효 토큰으로 거절되기 전에 회원이 새 토큰을 저장했으면 새 토큰은 지우지 않는다")
    fun reviewCompleteNotificationKeepsTokenReplacedBeforeResult() {
        // given
        insertToken(OWNER_ID, OWNER_TOKEN)
        willAnswer {
            fcmTokenRepository.createOrUpdate(OWNER_ID, NEW_OWNER_TOKEN)
            NotificationResult.INVALID_TOKEN
        }.given(notificationClient)
            .sendNotification(anyText(), anyText(), anyText(), anyNullableText(), anyText())
        val bodyPhotoId = insertBodyPhoto(OWNER_ID, reviewCount = 9, totalReviewScore = 36.0)
        insertQueue(REVIEWER_ID, bodyPhotoId, OWNER_ID)

        // when
        mockMvc.perform(reviewRequest(REVIEWER_ID, bodyPhotoId))
        awaitAsyncIdle()

        // then
        assertThat(notificationCount()).isEqualTo(1)
        assertThat(tokenOf(OWNER_ID)).isEqualTo(NEW_OWNER_TOKEN)
    }

    @Test
    @DisplayName("리뷰 요청 스케줄러는 무효 토큰으로 거절되기 전에 회원이 새 토큰을 저장했으면 새 토큰은 지우지 않는다")
    fun schedulerKeepsTokenReplacedBeforeResult() {
        // given
        listOf(OWNER_ID, REVIEWER_ID).forEach { insertSettings(it) }
        insertToken(OWNER_ID, OWNER_TOKEN)
        insertToken(REVIEWER_ID, REVIEWER_TOKEN)
        val ownerPhoto = insertBodyPhoto(OWNER_ID)
        val reviewerPhoto = insertBodyPhoto(REVIEWER_ID)
        insertQueue(OWNER_ID, reviewerPhoto, REVIEWER_ID)
        insertQueue(REVIEWER_ID, ownerPhoto, OWNER_ID)
        willAnswer { invocation ->
            if (invocation.getArgument<String>(0) == OWNER_TOKEN) {
                fcmTokenRepository.createOrUpdate(OWNER_ID, NEW_OWNER_TOKEN)
                NotificationResult.INVALID_TOKEN
            } else {
                NotificationResult.SENT
            }
        }.given(notificationClient).sendNotification(anyText(), anyText(), anyText(), anyNullableText(), anyText())

        // when
        notificationScheduler.notifyReviewable()

        // then
        assertThat(tokenOf(OWNER_ID)).isEqualTo(NEW_OWNER_TOKEN)
        assertThat(tokenOf(REVIEWER_ID)).isEqualTo(REVIEWER_TOKEN)
    }

    private fun respondWith(result: NotificationResult) {
        willAnswer { result }.given(notificationClient)
            .sendNotification(anyText(), anyText(), anyText(), anyNullableText(), anyText())
    }

    private fun anyNullableText(): String? {
        ArgumentMatchers.any<String>()
        return null
    }

    companion object {
        private const val OWNER_ID = 900_901L
        private const val REVIEWER_ID = 900_902L
        private const val OWNER_TOKEN = "owner-device-token"
        private const val REVIEWER_TOKEN = "reviewer-device-token"
        private const val NEW_OWNER_TOKEN = "new-owner-device-token"
    }

}
