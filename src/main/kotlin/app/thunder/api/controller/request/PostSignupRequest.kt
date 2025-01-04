package app.thunder.api.controller.request

import app.thunder.api.domain.member.Gender
import java.time.LocalDate

data class PostSignupRequest(
    val nickname: String,
    val mobileCountry: String,
    val mobileNumber: String,
    val gender: Gender,
    val birthDay: LocalDate,
    val countryCode: String,
    val marketingAgreement: Boolean,
)
