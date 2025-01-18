package app.thunder.api.domain.body

import java.time.LocalDateTime

class BodyPhoto private constructor(
    val bodyPhotoId: Long,
    val memberId: Long,
    val imageUrl: String,
    isReviewCompleted: Boolean,
    val createdAt: LocalDateTime,
    updatedAt: LocalDateTime?,
) {
    var isReviewCompleted: Boolean = isReviewCompleted
        private set
    var updatedAt: LocalDateTime? = updatedAt
        private set

    companion object {
        fun from(entity: BodyPhotoEntity): BodyPhoto {
            return BodyPhoto(
                entity.bodyPhotoId,
                entity.memberId,
                entity.imageUrl,
                entity.isReviewCompleted,
                entity.createdAt,
                entity.updatedAt,
            )
        }
    }

    fun completeReview() {
        this.isReviewCompleted = true
        this.updatedAt = LocalDateTime.now()
    }

}