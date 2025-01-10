package app.thunder.api.controller.request

data class PostSmsResetRequest(
    val deviceId: String? = null,
    val mobileNumber: String? = null,
)
