package app.thunder.api.event

import app.thunder.api.adapter.notification.NotificationAdapter
import app.thunder.api.domain.member.adapter.FcmTokenAdapter
import app.thunder.api.domain.review.adapter.ReviewableBodyPhotoAdapter
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionalEventListener

@Component
class GlobalEventHandler(
    private val reviewableBodyPhotoAdapter: ReviewableBodyPhotoAdapter,
    private val fcmTokenAdapter: FcmTokenAdapter,
    private val notificationAdapter: NotificationAdapter,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val REVIEW_COMPLETE_TITLE = "\uD83D\uDD25눈바디 측정 완료\uD83D\uDD25"
        private const val REVIEW_COMPLETE_BODY = "지금 바로 측정결과를 확인해보세요!"
    }

    @Async
    @TransactionalEventListener
    fun supplyReviewableBodyPhotos(event: SupplyReviewableEvent) {
        reviewableBodyPhotoAdapter.refresh(event.memberId)
    }

    @Async
    @TransactionalEventListener
    fun notifyReviewComplete(event: ReviewCompleteEvent) {
        fcmTokenAdapter.getByMemberId(event.memberId)
            ?.let { fcmToken ->
                notificationAdapter.sendMulticastNotification(
                    fcmToken,
                    REVIEW_COMPLETE_TITLE,
                    REVIEW_COMPLETE_BODY,
                    event.imageUrl,
                    "/bodyCheck/${event.bodyPhotoId}"
                )
            }
            ?: log.error("notifyReviewComplete failed: not found fcm token - memberId: ${event.memberId}")
    }

}