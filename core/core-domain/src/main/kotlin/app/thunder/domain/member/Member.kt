package app.thunder.domain.member

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class Member constructor(
    val memberId: Long,
    val nickname: String,
    val mobileNumber: String,
    val mobileCountry: String,
    val gender: Gender,
    val birthDay: LocalDate,
    val age: Int,
    val countryCode: String,
    val marketingAgreement: Boolean,
    val memberUuid: UUID,
    loggedOutAt: LocalDateTime?,
    val createdAt: LocalDateTime,
    updatedAt: LocalDateTime?,
    updatedBy: Long?
) {
    var loggedOutAt: LocalDateTime? = loggedOutAt
        private set
    var updatedAt: LocalDateTime? = updatedAt
        private set
    var updatedBy: Long? = updatedBy
        private set

    fun logout(memberId: Long) {
        this.loggedOutAt = LocalDateTime.now()
        this.updatedAt = LocalDateTime.now()
        this.updatedBy = memberId
    }

}
