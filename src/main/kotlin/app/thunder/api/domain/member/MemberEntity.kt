package app.thunder.api.domain.member

import jakarta.persistence.*
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
    var nickname: String = nickname
        protected set

    @Column(name = "mobile_number", unique = true)
    var mobileNumber: String = mobileNumber
        protected set

    @Column(name = "mobile_country")
    var mobileCountry: String = mobileCountry
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "gender")
    var gender: Gender = gender
        protected set

    @Column(name = "birth_day")
    var birthDay: LocalDate = birthDay
        protected set

    @Column(name = "country_code")
    var countryCode: String = countryCode
        protected set

    @Column(name = "marketing_agreement")
    var marketingAgreement: Boolean = marketingAgreement
        protected set

    @Column(name = "member_uuid", unique = true)
    val memberUuid: UUID = UUID.randomUUID()

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

}
