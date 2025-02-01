package app.thunder.api.application

import app.thunder.api.domain.body.ReviewableBodyPhotoAdapter
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
) {

    @Async
    @EventListener
    fun supplyReviewableBodyPhotos(event: SupplyReviewableEvent) {
        val reviewMemberId = event.memberId
        val currentSize = reviewableBodyPhotoAdapter.getAllByMemberId(reviewMemberId).size
        val supplySize = REVIEWABLE_QUEUE_MAXIMUM_SIZE - currentSize

        val reviewedBodyPhotoIdSet = bodyReviewAdapter.getAllByMemberId(reviewMemberId)
            .map { it.bodyPhotoId }.toSet()
        val filteredBodyPhotoList = bodyPhotoAdapter.getAllByReviewNotCompleted()
            .asSequence()
            .filter { it.memberId != reviewMemberId }
            .filter { !reviewedBodyPhotoIdSet.contains(it.bodyPhotoId) }
            // TODO: filter not blocked user
            // TODO: filter not flagged body Photo
            // TODO: sorted by reviewCount asc
            .shuffled()
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