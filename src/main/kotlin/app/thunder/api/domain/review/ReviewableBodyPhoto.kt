package app.thunder.api.domain.review

import app.thunder.api.domain.review.entity.ReviewableBodyPhotoEntity
import java.time.LocalDateTime

class ReviewableBodyPhoto private constructor(
    val memberId: Long,
    val bodyPhotoId: Long,
    val bodyPhotoMemberId: Long,
    val createdAt: LocalDateTime,
) {

    companion object {
        fun from(entity: ReviewableBodyPhotoEntity): ReviewableBodyPhoto {
            return ReviewableBodyPhoto(
                entity.memberId,
                entity.bodyPhotoId,
                entity.bodyPhotoMemberId,
                entity.createdAt,
            )
        }
    }

}