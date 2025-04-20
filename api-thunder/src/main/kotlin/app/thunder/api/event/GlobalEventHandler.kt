package app.thunder.api.event

import app.thunder.core.flag.repository.FlagHistoryRepository
import app.thunder.core.member.repository.FcmTokenRepository
import app.thunder.core.member.repository.MemberBlockRelationRepository
import app.thunder.core.member.repository.MemberRepository
import app.thunder.core.member.repository.MemberSettingRepository
import app.thunder.core.notification.client.NotificationClient
import app.thunder.core.photo.repository.BodyPhotoRepository
import app.thunder.core.review.repository.BodyReviewRepository
import app.thunder.core.review.repository.ReviewableBodyPhotoRepository
import app.thunder.core.review.command.CreateReviewableBodyPhotoCommand
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionalEventListener
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Component
class GlobalEventHandler(
    private val reviewableBodyPhotoRepository: ReviewableBodyPhotoRepository,
    private val fcmTokenRepository: FcmTokenRepository,
    private val notificationClient: NotificationClient,
    private val memberSettingRepository: MemberSettingRepository,
    private val memberRepository: MemberRepository,
    private val bodyPhotoRepository: BodyPhotoRepository,
    private val bodyReviewRepository: BodyReviewRepository,
    private val flagHistoryRepository: FlagHistoryRepository,
    private val memberBlockRelationRepository: MemberBlockRelationRepository,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener
    fun supplyReviewableBodyPhotos(event: RefreshReviewableEvent) {
        refresh(event.memberId)
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener
    fun handleReviewUploadEvent(event: ReviewUploadEvent) {
        val notCompletedPhotos = bodyPhotoRepository.getNotReviewCompletedAll()
        val flagCountMap = flagHistoryRepository.getFlagCountByBodyPhotoId()

        memberRepository.getAll().forEach { member ->
            refreshWithPreloadedData(member.memberId, notCompletedPhotos, flagCountMap)
        }
    }

    private fun refresh(reviewMemberId: Long) {
        val notCompletedPhotos = bodyPhotoRepository.getNotReviewCompletedAll()
        val flagCountMap = flagHistoryRepository.getFlagCountByBodyPhotoId()
        refreshWithPreloadedData(reviewMemberId, notCompletedPhotos, flagCountMap)
    }

    private fun refreshWithPreloadedData(
        reviewMemberId: Long,
        notCompletedPhotos: List<app.thunder.core.photo.domain.BodyPhoto>,
        flagCountMap: Map<Long, Int>,
    ) {
        val suppliedBodyPhotoIdSet = reviewableBodyPhotoRepository.getAllByMemberId(reviewMemberId)
            .map { it.bodyPhotoId }.toSet()
        if (suppliedBodyPhotoIdSet.size > REVIEWABLE_QUEUE_MINIMUM_SIZE) {
            return
        }
        val supplySize = REVIEWABLE_QUEUE_MAXIMUM_SIZE - suppliedBodyPhotoIdSet.size

        val flaggedBodyPhotoIdSet = flagHistoryRepository.getAllByMemberId(reviewMemberId)
            .map { it.bodyPhotoId }.toHashSet()

        val reviewedBodyPhotoIdSet = bodyReviewRepository.getAllByMemberId(reviewMemberId)
            .map { it.bodyPhotoId }.toSet()
        val blockedMemberIdSet = memberBlockRelationRepository.getBlockedMemberIdsByMemberId(reviewMemberId)

        val filteredBodyPhotoList = notCompletedPhotos
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
        reviewableBodyPhotoRepository.saveAll(commands)
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener
    fun notifyReviewComplete(event: ReviewCompleteEvent) {
        val isNotificationDisagree = memberSettingRepository.getByMemberId(event.memberId)?.reviewCompleteNotify == false
        if (isNotificationDisagree) {
            return
        }

        reviewableBodyPhotoRepository.deleteAllByBodyPhotoId(event.bodyPhotoId)

        fcmTokenRepository.getByMemberId(event.memberId)
            ?.let { fcmToken ->
                notificationClient.sendNotification(
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
