package app.thunder.api.controller.response

import java.time.ZonedDateTime

data class GetBodyPhotoResponse(
    val bodyPhotoId: Long,
    val imageUrl: String,
    val isReviewCompleted: Boolean,
    val reviewScore: Double,
    val createdAt: ZonedDateTime,
)
