package app.thunder.api.controller.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class PostSmsVerifyRequest(
    @field:NotBlank
    val deviceId: String,

    @field:NotBlank
    @field:Pattern(regexp = "^[0-9]{10,15}$")
    val mobileNumber: String,

    @field:NotBlank
    @field:Size(max = 2)
    val mobileCountry: String,

    @field:NotBlank
    val verificationCode: String
)
