package app.thunder.api.controller.response

import app.thunder.api.domain.member.Gender
import java.time.ZonedDateTime

data class GetBodyPhotoResultResponse(
    val bodyPhotoId: Long,
    val imageUrl: String,
    val isReviewCompleted: Boolean,
    val progressRate: Double,
    val gender: Gender,
    val totalScore: Double,
    val genderTopPercent: Double,
    val genderTopRate: Double,
    val createdAt: ZonedDateTime,
)
