package app.thunder.domain.member.command

import app.thunder.domain.member.Gender
import java.time.LocalDate

data class CreateMemberCommand(
    val nickname: String,
    val mobileCountry: String,
    val mobileNumber: String,
    val gender: Gender,
    val birthDay: LocalDate,
    val countryCode: String,
    val marketingAgreement: Boolean,
)
