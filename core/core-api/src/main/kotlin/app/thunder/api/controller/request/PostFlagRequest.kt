package app.thunder.api.controller.request

import app.thunder.domain.flag.FlagReason
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size

data class PostFlagRequest(
    @field:NotNull @field:Positive
    val bodyPhotoId: Long,

    @field:NotNull
    val flagReason: FlagReason,

    @field:Size(max = 200)
    val otherReason: String?,
)
