package app.thunder.api.controller.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class PostSmsRequest(
    @field:NotBlank
    val deviceId: String,

    @field:NotBlank
    @field:Size(min = 10, max = 15)
    val mobileNumber: String,

    @field:NotBlank
    @field:Size(max = 2)
    val mobileCountry: String,

    val isTestMode: Boolean = false,
)
