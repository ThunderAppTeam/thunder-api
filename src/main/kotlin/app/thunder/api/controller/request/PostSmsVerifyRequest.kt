package app.thunder.api.controller.request

data class PostSmsVerifyRequest(
    val deviceId: String,
    val mobileNumber: String,
    val mobileCountry: String,
    val verificationCode: String
)
