package app.thunder.api.application

import app.thunder.api.domain.body.ReviewableBodyPhotoAdapter
import app.thunder.api.domain.flag.FlagHistoryAdapter
import app.thunder.api.domain.photo.BodyPhotoAdapter
import app.thunder.api.domain.review.BodyReviewAdapter
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

@Component
class SupplyReviewableEventHandler(
    private val reviewableBodyPhotoAdapter: ReviewableBodyPhotoAdapter,
    private val bodyPhotoAdapter: BodyPhotoAdapter,
    private val bodyReviewAdapter: BodyReviewAdapter,
    private val flagHistoryAdapter: FlagHistoryAdapter,
) {

    @Async
    @EventListener
    fun supplyReviewableBodyPhotos(event: SupplyReviewableEvent) {
        val reviewMemberId = event.memberId
        val suppliedBodyPhotoIdSet = reviewableBodyPhotoAdapter.getAllByMemberId(reviewMemberId)
            .map { it.bodyPhotoId }.toSet()
        val supplySize = REVIEWABLE_QUEUE_MAXIMUM_SIZE - suppliedBodyPhotoIdSet.size

        val reviewedBodyPhotoIdSet = bodyReviewAdapter.getAllByMemberId(reviewMemberId)
            .map { it.bodyPhotoId }.toSet()
        val flaggedBodyPhotoIdSet = flagHistoryAdapter.getAllByMemberId(reviewMemberId)
            .map { it.bodyPhotoId }.toSet()

        val filteredBodyPhotoList = bodyPhotoAdapter.getAll()
            .asSequence()
            .filter { !it.isReviewCompleted() }
            .filter { it.memberId != reviewMemberId }
            .filter { !suppliedBodyPhotoIdSet.contains(it.bodyPhotoId) }
            .filter { !reviewedBodyPhotoIdSet.contains(it.bodyPhotoId) }
            .filter { !flaggedBodyPhotoIdSet.contains(it.bodyPhotoId) }
            // TODO: filter not blocked user
            .shuffled()
            .sortedBy { it.reviewScore }
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