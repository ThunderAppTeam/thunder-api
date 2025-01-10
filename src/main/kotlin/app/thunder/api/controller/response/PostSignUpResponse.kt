package app.thunder.api.controller.response

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class PostSignUpResponse(
    val memberId: Long?,
    val accessToken: String? = null,
)
