package app.thunder.api.controller.request

data class PostSmsRequest(
    val mobileNumber: String,
    val mobileCountry: String,
)
