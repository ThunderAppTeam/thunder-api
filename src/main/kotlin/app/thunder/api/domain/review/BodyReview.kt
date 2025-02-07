package app.thunder.api.domain.review

import app.thunder.api.domain.review.entity.BodyReviewEntity
import java.time.LocalDateTime

data class BodyReview(
    val bodyReviewId: Long,
    val bodyPhotoId: Long,
    val memberId: Long,
    val score: Int,
    val createdAt: LocalDateTime
) {

    companion object {
        fun from(entity: BodyReviewEntity): BodyReview {
            return BodyReview(
                entity.bodyReviewId,
                entity.bodyPhotoId,
                entity.memberId,
                entity.score,
                entity.createdAt
            )
        }
    }

}