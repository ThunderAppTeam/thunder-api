package app.thunder.api.event

import app.thunder.core.flag.repository.FlagHistoryRepository
import app.thunder.core.member.repository.FcmTokenRepository
import app.thunder.core.member.repository.MemberRepository
import app.thunder.core.member.repository.MemberSettingRepository
import app.thunder.core.notification.client.NotificationClient
import app.thunder.core.notification.domain.NotificationResult
import app.thunder.core.photo.repository.BodyPhotoRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionalEventListener

@Component
class GlobalEventHandler(
    private val reviewableQueueRefresher: ReviewableQueueRefresher,
    private val fcmTokenRepository: FcmTokenRepository,
    private val notificationClient: NotificationClient,
    private val memberSettingRepository: MemberSettingRepository,
    private val memberRepository: MemberRepository,
    private val bodyPhotoRepository: BodyPhotoRepository,
    private val flagHistoryRepository: FlagHistoryRepository,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Async
    @TransactionalEventListener
    fun supplyReviewableBodyPhotos(event: RefreshReviewableEvent) {
        val notCompletedPhotos = bodyPhotoRepository.getNotReviewCompletedAll()
        val flagCountMap = flagHistoryRepository.getFlagCountByBodyPhotoId()
        reviewableQueueRefresher.refresh(event.memberId, notCompletedPhotos, flagCountMap)
    }

    @Async
    @TransactionalEventListener
    fun handleReviewUploadEvent(event: ReviewUploadEvent) {
        val notCompletedPhotos = bodyPhotoRepository.getNotReviewCompletedAll()
        val flagCountMap = flagHistoryRepository.getFlagCountByBodyPhotoId()

        memberRepository.getAll().forEach { member ->
            runCatching { reviewableQueueRefresher.refresh(member.memberId, notCompletedPhotos, flagCountMap) }
                .onFailure { log.error("refresh failed - memberId: ${member.memberId}", it) }
        }
    }

    @Async
    @TransactionalEventListener
    fun notifyReviewComplete(event: ReviewCompleteEvent) {
        val isNotificationDisagree = memberSettingRepository.getByMemberId(event.memberId)?.reviewCompleteNotify == false
        if (isNotificationDisagree) {
            return
        }

        fcmTokenRepository.getByMemberId(event.memberId)
            ?.let { fcmToken ->
                val result = notificationClient.sendNotification(
                    fcmToken = fcmToken,
                    title = "\uD83D\uDD25눈바디 측정 완료\uD83D\uDD25",
                    body = "지금 바로 측정결과를 확인해보세요!",
                    imageUrl = event.imageUrl,
                    routePath = "/bodyCheck/${event.bodyPhotoId}"
                )
                if (result == NotificationResult.INVALID_TOKEN) {
                    fcmTokenRepository.deleteByMemberIdAndFcmToken(event.memberId, fcmToken)
                }
            }
            ?: log.error("notifyReviewComplete failed: not found fcm token - memberId: ${event.memberId}")
    }

}
