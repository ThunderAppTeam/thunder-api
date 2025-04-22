package app.thunder.api.event

import app.thunder.core.flag.repository.FlagHistoryRepository
import app.thunder.core.member.repository.MemberBlockRelationRepository
import app.thunder.core.member.repository.MemberRepository
import app.thunder.core.photo.domain.BodyPhoto
import app.thunder.core.photo.repository.BodyPhotoRepository
import app.thunder.core.review.command.CreateReviewableBodyPhotoCommand
import app.thunder.core.review.repository.BodyReviewRepository
import app.thunder.core.review.repository.ReviewableBodyPhotoRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Component
class ReviewableQueueRefresher(
    private val reviewableBodyPhotoRepository: ReviewableBodyPhotoRepository,
    private val bodyPhotoRepository: BodyPhotoRepository,
    private val memberRepository: MemberRepository,
    private val bodyReviewRepository: BodyReviewRepository,
    private val flagHistoryRepository: FlagHistoryRepository,
    private val memberBlockRelationRepository: MemberBlockRelationRepository,
) {

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun refresh(memberId: Long, notCompletedPhotos: List<BodyPhoto>, flagCountMap: Map<Long, Int>) {
        reviewableBodyPhotoRepository.lockQueue(memberId)
        memberRepository.getById(memberId) ?: return

        val flaggedBodyPhotoIdSet = flagHistoryRepository.getAllByMemberId(memberId)
            .map { it.bodyPhotoId }.toHashSet()
        val reviewedBodyPhotoIdSet = bodyReviewRepository.getAllByMemberId(memberId)
            .map { it.bodyPhotoId }.toSet()
        val blockedMemberIdSet = memberBlockRelationRepository.getBlockedMemberIdsByMemberId(memberId)
        val isSupplyable = { bodyPhoto: BodyPhoto ->
            bodyPhoto.memberId != memberId &&
                !reviewedBodyPhotoIdSet.contains(bodyPhoto.bodyPhotoId) &&
                !flaggedBodyPhotoIdSet.contains(bodyPhoto.bodyPhotoId) &&
                (flagCountMap[bodyPhoto.bodyPhotoId] ?: 0) < FLAG_LIMIT &&
                !blockedMemberIdSet.contains(bodyPhoto.memberId)
        }

        val suppliedBodyPhotoIdSet = removeStaleAndGetRemaining(memberId, isSupplyable)
        if (suppliedBodyPhotoIdSet.size > QUEUE_MINIMUM_SIZE) {
            return
        }
        val supplySize = QUEUE_MAXIMUM_SIZE - suppliedBodyPhotoIdSet.size

        val candidates = notCompletedPhotos
            .asSequence()
            .filter { !suppliedBodyPhotoIdSet.contains(it.bodyPhotoId) }
            .filter(isSupplyable)
            .shuffled()
            .sortedBy { it.reviewCount }
            .take(supplySize)
            .toList()
        val commands = currentlyReviewable(candidates)
            .map { bodyPhoto ->
                CreateReviewableBodyPhotoCommand(
                    memberId = memberId,
                    bodyPhotoId = bodyPhoto.bodyPhotoId,
                    bodyPhotoMemberId = bodyPhoto.memberId,
                )
            }
        reviewableBodyPhotoRepository.saveAll(commands)
    }

    private fun removeStaleAndGetRemaining(memberId: Long, isSupplyable: (BodyPhoto) -> Boolean): Set<Long> {
        val queuedBodyPhotoIds = reviewableBodyPhotoRepository.getAllByMemberId(memberId).map { it.bodyPhotoId }
        val validBodyPhotoIdSet = bodyPhotoRepository.getAllById(queuedBodyPhotoIds)
            .filterNot { it.isReviewCompleted() }
            .filter(isSupplyable)
            .map { it.bodyPhotoId }
            .toSet()
        queuedBodyPhotoIds
            .filterNot { validBodyPhotoIdSet.contains(it) }
            .forEach { reviewableBodyPhotoRepository.deleteByMemberIdAndBodyPhotoId(memberId, it) }
        return validBodyPhotoIdSet
    }

    private fun currentlyReviewable(bodyPhotos: List<BodyPhoto>): List<BodyPhoto> {
        return bodyPhotoRepository.getAllById(bodyPhotos.map { it.bodyPhotoId })
            .filterNot { it.isReviewCompleted() }
    }

    companion object {
        private const val QUEUE_MAXIMUM_SIZE: Int = 30
        private const val QUEUE_MINIMUM_SIZE: Int = 10
        private const val FLAG_LIMIT: Int = 3
    }

}
