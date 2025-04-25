package app.thunder.api.controller.request

import app.thunder.core.member.domain.Gender
import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Past
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
    @field:Pattern(regexp = "^[0-9]{10,15}$")
    val mobileNumber: String,

    val gender: Gender,

    @field:Past
    @field:DateTimeFormat(pattern = "yyyy-MM-dd")
    val birthDay: LocalDate,

    @field:Size(max = 2)
    val countryCode: String,

    @JsonProperty(required = true)
    val marketingAgreement: Boolean,
)
