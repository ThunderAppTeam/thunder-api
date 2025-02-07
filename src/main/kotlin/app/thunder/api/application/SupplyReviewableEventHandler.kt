package app.thunder.api.application

import app.thunder.api.domain.review.adapter.ReviewableBodyPhotoAdapter
import app.thunder.api.domain.flag.FlagHistoryAdapter
import app.thunder.api.domain.member.adapter.MemberBlockRelationAdapter
import app.thunder.api.domain.photo.BodyPhotoAdapter
import app.thunder.api.domain.review.adapter.BodyReviewAdapter
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

@Component
class SupplyReviewableEventHandler(
    private val reviewableBodyPhotoAdapter: ReviewableBodyPhotoAdapter,
    private val bodyPhotoAdapter: BodyPhotoAdapter,
    private val bodyReviewAdapter: BodyReviewAdapter,
    private val flagHistoryAdapter: FlagHistoryAdapter,
    private val memberBlockRelationAdapter: MemberBlockRelationAdapter,
) {

    @Async
    @EventListener
    fun supplyReviewableBodyPhotos(event: SupplyReviewableEvent) {
        val reviewMemberId = event.memberId
        val suppliedBodyPhotoIdSet = reviewableBodyPhotoAdapter.getAllByMemberId(reviewMemberId)
            .map { it.bodyPhotoId }.toSet()
        val supplySize = REVIEWABLE_QUEUE_MAXIMUM_SIZE - suppliedBodyPhotoIdSet.size

        val flagCountMap = hashMapOf<Long, Int>()
        val flaggedBodyPhotoIdSet = hashSetOf<Long>()
        flagHistoryAdapter.getAll().forEach { flagHistory ->
            flagCountMap.merge(flagHistory.bodyPhotoId, 1, Int::plus)
            if (flagHistory.memberId == reviewMemberId) {
                flaggedBodyPhotoIdSet.add(flagHistory.bodyPhotoId)
            }
        }

        val reviewedBodyPhotoIdSet = bodyReviewAdapter.getAllByMemberId(reviewMemberId)
            .map { it.bodyPhotoId }.toSet()
        val blockedMemberIdSet = memberBlockRelationAdapter.getByMemberId(reviewMemberId)
            .map { it.blockedMemberId }.toSet()

        val filteredBodyPhotoList = bodyPhotoAdapter.getAllNotReviewCompleted()
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

        filteredBodyPhotoList.forEach {
            reviewableBodyPhotoAdapter.create(memberId = reviewMemberId,
                                              bodyPhotoId = it.bodyPhotoId,
                                              bodyPhotoMemberId = it.memberId)
        }
    }

    companion object {
        private const val REVIEWABLE_QUEUE_MAXIMUM_SIZE: Int = 30
        const val REVIEWABLE_QUEUE_MINIMUM_SIZE: Int = 10
    }

}