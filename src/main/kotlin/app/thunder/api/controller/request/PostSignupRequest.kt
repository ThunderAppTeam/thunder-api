package app.thunder.api.controller.request

import java.time.LocalDate

data class PostSignupRequest(
    val nickname: String,
    val mobileCountry: String,
    val mobileNumber: String,
    val gender: String,
    val birthDay: LocalDate,
    val countryCode: String,
    val marketingAgreement: Boolean,
)
