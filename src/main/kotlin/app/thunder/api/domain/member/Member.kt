package app.thunder.api.domain.member

import app.thunder.api.domain.member.entity.MemberEntity
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Period
import java.util.UUID

class Member private constructor(
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

    companion object {
        fun from(entity: MemberEntity): Member {
            return Member(
                entity.memberId,
                entity.nickname,
                entity.mobileNumber,
                entity.mobileCountry,
                entity.gender,
                entity.birthDay,
                Period.between(entity.birthDay, LocalDate.now()).years,
                entity.countryCode,
                entity.marketingAgreement,
                entity.memberUuid,
                entity.loggedOutAt,
                entity.createdAt,
                entity.updatedAt,
                entity.updatedBy
            )
        }
    }

    fun logout(memberId: Long) {
        this.loggedOutAt = LocalDateTime.now()
        this.updatedAt = LocalDateTime.now()
        this.updatedBy = memberId
    }

}
