package app.thunder.api.controller.request

import app.thunder.core.member.domain.MemberDeletionReason
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class DeleteMemberRequest(
    @field:NotNull
    val deletionReason: MemberDeletionReason,

    @field:Size(max = 200)
    val otherReason: String?,
)
