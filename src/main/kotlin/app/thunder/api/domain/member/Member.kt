package app.thunder.api.domain.member

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Period
import java.util.UUID

data class Member private constructor(
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
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime?,
    val updatedBy: Long?
) {

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
                entity.createdAt,
                entity.updatedAt,
                entity.updatedBy
            )
        }
    }

}
