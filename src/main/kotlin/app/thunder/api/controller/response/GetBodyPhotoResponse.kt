package app.thunder.api.controller.response

import java.time.ZonedDateTime

data class GetBodyPhotoResponse(
    val bodyPhotoId: Long,
    val imageUrl: String,
    val isReviewCompleted: Boolean,
    val totalScore: Double,
    val genderTopPercent: Double,
    val createdAt: ZonedDateTime,
)
