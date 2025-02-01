package app.thunder.api.controller.request

import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive

data class PostMemberBlockRequest(
    @field:NotNull @field:Positive
    val blockedMemberId: Long,
)
