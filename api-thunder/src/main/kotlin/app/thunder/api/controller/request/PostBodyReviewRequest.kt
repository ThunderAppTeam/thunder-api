package app.thunder.api.controller.request

import jakarta.validation.constraints.Positive
import org.hibernate.validator.constraints.Range

data class PostBodyReviewRequest(
    @field:Positive
    val bodyPhotoId: Long,
    @field:Range(min = 1, max = 5)
    val score: Int,
)
