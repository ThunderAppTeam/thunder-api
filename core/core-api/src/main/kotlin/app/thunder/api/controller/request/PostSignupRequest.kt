package app.thunder.api.controller.request

import app.thunder.domain.member.Gender
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.LocalDate
import org.springframework.format.annotation.DateTimeFormat

data class PostSignupRequest(
    @field:Pattern(regexp = "^[a-zA-Z0-9가-힣]{2,8}$")
    val nickname: String,

    @field:NotBlank
    @field:Size(max = 2)
    val mobileCountry: String,

    @field:NotBlank
    @field:Size(min = 10, max = 15)
    val mobileNumber: String,

    val gender: Gender,

    @field:DateTimeFormat(pattern = "yyyy-MM-dd")
    val birthDay: LocalDate,

    @field:Size(max = 2)
    val countryCode: String,

    val marketingAgreement: Boolean,
)
