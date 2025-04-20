package app.thunder.api.controller.request

import jakarta.validation.constraints.NotBlank

data class PostFcmTokenRequest(
    @NotBlank
    val fcmToken: String,
)
