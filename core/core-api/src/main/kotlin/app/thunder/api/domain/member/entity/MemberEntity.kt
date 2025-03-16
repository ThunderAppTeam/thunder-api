package app.thunder.api.domain.member.entity

import app.thunder.domain.member.Gender
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Table(name = "member")
@Entity
class MemberEntity private constructor(
    nickname: String,
    mobileNumber: String,
    mobileCountry: String,
    gender: Gender,
    birthDay: LocalDate,
    countryCode: String,
    marketingAgreement: Boolean,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    val memberId: Long = 0

    @Column(name = "nickname", unique = true)
    val nickname: String = nickname

    @Column(name = "mobile_number", unique = true)
    val mobileNumber: String = mobileNumber

    @Column(name = "mobile_country")
    val mobileCountry: String = mobileCountry

    @Enumerated(EnumType.STRING)
    @Column(name = "gender")
    val gender: Gender = gender

    @Column(name = "birth_day")
    val birthDay: LocalDate = birthDay

    @Column(name = "country_code")
    val countryCode: String = countryCode

    @Column(name = "marketing_agreement")
    val marketingAgreement: Boolean = marketingAgreement

    @Column(name = "member_uuid", unique = true)
    val memberUuid: UUID = UUID.randomUUID()

    @Column(name = "logged_out_at")
    var loggedOutAt: LocalDateTime? = null
        protected set

    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now()

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null
        protected set

    @Column(name = "updated_by")
    var updatedBy: Long? = null
        protected set


    companion object {
        fun create(
            nickname: String,
            mobileNumber: String,
            mobileCountry: String,
            gender: Gender,
            birthDay: LocalDate,
            countryCode: String,
            marketingAgreement: Boolean
        ): MemberEntity {
            return MemberEntity(
                nickname = nickname,
                mobileNumber = mobileNumber,
                mobileCountry = mobileCountry,
                gender = gender,
                birthDay = birthDay,
                countryCode = countryCode,
                marketingAgreement = marketingAgreement,
            )
        }
    }

    fun update(loggedOutAt: LocalDateTime?, updatedAt: LocalDateTime?, updatedBy: Long?) {
        this.loggedOutAt = loggedOutAt
        this.updatedAt = updatedAt
        this.updatedBy = updatedBy
    }

}
