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

    @Async
    @TransactionalEventListener
    fun supplyReviewableBodyPhotos(event: RefreshReviewableEvent) {
        reviewableBodyPhotoAdapter.refresh(event.memberId)
    }

    @Async
    @TransactionalEventListener
    fun notifyReviewComplete(event: ReviewCompleteEvent) {
        fcmTokenAdapter.getByMemberId(event.memberId)
            ?.let { fcmToken ->
                notificationAdapter.sendNotification(
                    token = fcmToken,
                    title = "\uD83D\uDD25눈바디 측정 완료\uD83D\uDD25",
                    body = "지금 바로 측정결과를 확인해보세요!",
                    imageUrl = event.imageUrl,
                    routePath = "/bodyCheck/${event.bodyPhotoId}"
                )
            }
            ?: log.error("notifyReviewComplete failed: not found fcm token - memberId: ${event.memberId}")
    }

}