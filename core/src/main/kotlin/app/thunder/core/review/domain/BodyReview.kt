package app.thunder.core.review.domain

import java.time.LocalDateTime

data class BodyReview(
    val bodyReviewId: Long,
    val bodyPhotoId: Long,
    val memberId: Long,
    val score: Int,
    val createdAt: LocalDateTime
)
