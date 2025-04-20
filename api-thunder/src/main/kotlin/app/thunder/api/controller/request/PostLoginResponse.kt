package app.thunder.api.controller.request

import app.thunder.core.member.domain.Gender
import java.util.UUID

data class PostLoginResponse(
    val memberId: Long?,
    val memberUuid: UUID?,
    val age: Int?,
    val gender: Gender?,
    val accessToken: String?,
)
