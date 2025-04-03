package app.thunder.api.controller.response

import app.thunder.domain.member.Gender
import com.fasterxml.jackson.annotation.JsonInclude
import java.util.UUID

@JsonInclude(JsonInclude.Include.NON_NULL)
data class PostSignUpResponse(
    val memberId: Long?,
    val memberUuid: UUID?,
    val age: Int?,
    val gender: Gender?,
    val accessToken: String? = null,
    val refreshToken: String? = null,
)
