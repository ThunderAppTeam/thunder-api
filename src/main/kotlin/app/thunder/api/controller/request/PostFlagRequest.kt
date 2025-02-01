package app.thunder.api.controller.request

import app.thunder.api.domain.flag.FlagReason
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive

data class PostFlagRequest(
    @field:NotNull @field:Positive
    val bodyPhotoId: Long,

    @field:NotNull
    val flagReason: FlagReason,

    val otherReason: String?,
)
