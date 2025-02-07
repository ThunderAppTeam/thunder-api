package app.thunder.api.controller.request

import app.thunder.api.domain.flag.FlagReason
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import org.hibernate.validator.constraints.Range

data class PostFlagRequest(
    @field:NotNull @field:Positive
    val bodyPhotoId: Long,

    @field:NotNull
    val flagReason: FlagReason,

    @field:Range(min = 0, max = 200)
    val otherReason: String?,
)
