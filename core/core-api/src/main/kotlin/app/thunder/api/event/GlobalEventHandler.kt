package app.thunder.api.event

import app.thunder.api.adapter.notification.NotificationAdapter
import app.thunder.api.domain.flag.FlagHistoryRepository
import app.thunder.api.domain.member.adapter.FcmTokenAdapter
import app.thunder.api.domain.member.adapter.MemberAdapter
import app.thunder.api.domain.member.adapter.MemberSettingAdapter
import app.thunder.api.domain.member.repository.MemberBlockRelationRepository
import app.thunder.domain.photo.BodyPhotoAdapter
import app.thunder.domain.review.BodyReviewAdapter
import app.thunder.domain.review.ReviewableBodyPhotoAdapter
import app.thunder.domain.review.command.CreateReviewableBodyPhotoCommand
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionalEventListener

@Component
class GlobalEventHandler(
    private val reviewableBodyPhotoAdapter: ReviewableBodyPhotoAdapter,
    private val fcmTokenAdapter: FcmTokenAdapter,
    private val notificationAdapter: NotificationAdapter,
    private val memberSettingAdapter: MemberSettingAdapter,
    private val memberAdapter: MemberAdapter,
    private val bodyPhotoAdapter: BodyPhotoAdapter,
    private val bodyReviewAdapter: BodyReviewAdapter,
    private val flagHistoryRepository: FlagHistoryRepository,
    private val memberBlockRelationRepository: MemberBlockRelationRepository,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Async
    @TransactionalEventListener
    fun supplyReviewableBodyPhotos(event: RefreshReviewableEvent) {
        refresh(event.memberId)
    }

    @Async
    @TransactionalEventListener
    fun handleReviewUploadEvent(event: ReviewUploadEvent) {
        memberAdapter.getAll().forEach { member ->
            refresh(member.memberId)
        }
    }

    private fun refresh(reviewMemberId: Long) {
        val suppliedBodyPhotoIdSet = reviewableBodyPhotoAdapter.getAllByMemberId(reviewMemberId)
            .map { it.bodyPhotoId }.toSet()
        if (suppliedBodyPhotoIdSet.size > REVIEWABLE_QUEUE_MINIMUM_SIZE) {
            return
        }
        val supplySize = REVIEWABLE_QUEUE_MAXIMUM_SIZE - suppliedBodyPhotoIdSet.size

        val flagCountMap = hashMapOf<Long, Int>()
        val flaggedBodyPhotoIdSet = hashSetOf<Long>()
        flagHistoryRepository.findAll().forEach { flagHistory ->
            flagCountMap.merge(flagHistory.bodyPhotoId, 1, Int::plus)
            if (flagHistory.memberId == reviewMemberId) {
                flaggedBodyPhotoIdSet.add(flagHistory.bodyPhotoId)
            }
        }

        val reviewedBodyPhotoIdSet = bodyReviewAdapter.getAllByMemberId(reviewMemberId)
            .map { it.bodyPhotoId }.toSet()
        val blockedMemberIdSet = memberBlockRelationRepository.findAllByMemberId(reviewMemberId)
            .map { it.blockedMemberId }.toSet()

        val filteredBodyPhotoList = bodyPhotoAdapter.getNotReviewCompletedAll()
            .asSequence()
            .filter { it.memberId != reviewMemberId }
            .filter { !suppliedBodyPhotoIdSet.contains(it.bodyPhotoId) }
            .filter { !reviewedBodyPhotoIdSet.contains(it.bodyPhotoId) }
            .filter { !flaggedBodyPhotoIdSet.contains(it.bodyPhotoId) }
            .filter { (flagCountMap[it.bodyPhotoId] ?: 0) < 3 }
            .filter { !blockedMemberIdSet.contains(it.memberId) }
            .shuffled()
            .sortedBy { it.reviewCount }
            .take(supplySize)
            .toList()

        val commands = filteredBodyPhotoList.map { bodyPhoto ->
            CreateReviewableBodyPhotoCommand(
                memberId = reviewMemberId,
                bodyPhotoId = bodyPhoto.bodyPhotoId,
                bodyPhotoMemberId = bodyPhoto.memberId
            )
        }
        reviewableBodyPhotoAdapter.saveAll(commands)
    }

    @Async
    @TransactionalEventListener
    fun notifyReviewComplete(event: ReviewCompleteEvent) {
        val isNotificationDisagree = memberSettingAdapter.getByMemberId(event.memberId)?.reviewCompleteNotify == false
        if (isNotificationDisagree) {
            return
        }

        reviewableBodyPhotoAdapter.deleteAllByBodyPhotoId(event.bodyPhotoId)

        fcmTokenAdapter.getByMemberId(event.memberId)
            ?.let { fcmToken ->
                notificationAdapter.sendNotification(
                    fcmToken = fcmToken,
                    title = "\uD83D\uDD25눈바디 측정 완료\uD83D\uDD25",
                    body = "지금 바로 측정결과를 확인해보세요!",
                    imageUrl = event.imageUrl,
                    routePath = "/bodyCheck/${event.bodyPhotoId}"
                )
            }
            ?: log.error("notifyReviewComplete failed: not found fcm token - memberId: ${event.memberId}")
    }


    companion object {
        private const val REVIEWABLE_QUEUE_MAXIMUM_SIZE: Int = 30
        private const val REVIEWABLE_QUEUE_MINIMUM_SIZE: Int = 10
    }

}