package app.thunder.api.controller.request

data class PostSmsVerifyRequest(
    val mobileNumber: String,
    val mobileCountry: String,
    val verificationCode: String
)
