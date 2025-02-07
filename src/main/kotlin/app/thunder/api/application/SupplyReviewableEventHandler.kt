package app.thunder.api.application

import app.thunder.api.domain.review.adapter.ReviewableBodyPhotoAdapter
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionalEventListener

@Component
class SupplyReviewableEventHandler(
    private val reviewableBodyPhotoAdapter: ReviewableBodyPhotoAdapter,
) {

    @Async
    @TransactionalEventListener
    fun supplyReviewableBodyPhotos(event: SupplyReviewableEvent) {
        reviewableBodyPhotoAdapter.refresh(event.memberId)
    }

}