package app.thunder.api.controller.request

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class PostLoginResponse(
    val accessToken: String?
)
