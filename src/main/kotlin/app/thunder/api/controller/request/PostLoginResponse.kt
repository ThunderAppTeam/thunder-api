package app.thunder.api.controller.request

data class PostLoginResponse(
    val memberId: Long?,
    val accessToken: String?,
)
