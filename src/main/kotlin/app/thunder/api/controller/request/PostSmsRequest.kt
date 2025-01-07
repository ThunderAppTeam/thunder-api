package app.thunder.api.controller.request

data class PostSmsRequest(
    val deviceId: String,
    val mobileNumber: String,
    val mobileCountry: String,
)
