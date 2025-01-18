package app.thunder.api.domain.body

import java.time.LocalDateTime

class ReviewRotation private constructor(
    val reviewRotationId: Long,
    val bodyPhotoId: Long,
    reviewedMemberIds: Set<Long>,
    val createdAt: LocalDateTime
) {
    var reviewedMemberIds: Set<Long> = reviewedMemberIds

    companion object {
        fun from(entity: ReviewRotationEntity): ReviewRotation {
            return ReviewRotation(
                entity.reviewRotationId,
                entity.bodyPhotoId,
                entity.reviewedMemberIds,
                entity.createdAt
            )
        }
    }

    fun addReviewedMember(memberId: Long) {
        this.reviewedMemberIds += memberId
    }

}