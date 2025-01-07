package app.thunder.api.domain.member

import jakarta.persistence.*
import java.time.LocalDateTime

@Table(name = "mobile_verification")
@Entity
class MobileVerificationEntity private constructor(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mobile_verification_id")
    val mobileVerificationId: Long = 0,

    @Column(name = "device_id")
    val deviceId: String,

    @Column(name = "mobile_number")
    val mobileNumber: String,

    @Column(name = "mobile_country")
    val mobileCountry: String,

    @Column(name = "verification_code")
    val verificationCode: String,

    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "expired_at")
    val expiredAt: LocalDateTime = createdAt.plusMinutes(3)
) {

    @Column(name = "verified_at")
    var verifiedAt: LocalDateTime? = null
        protected set

    companion object {
        fun create(
            deviceId: String,
            mobileNumber: String,
            mobileCountry: String,
            verificationCode: String
        ): MobileVerificationEntity {
            return MobileVerificationEntity(
                deviceId = deviceId,
                mobileNumber = mobileNumber,
                mobileCountry = mobileCountry,
                verificationCode = verificationCode
            )
        }
    }

    fun isExpired(): Boolean {
        return LocalDateTime.now().isAfter(expiredAt)
    }

    fun verified() {
        this.verifiedAt = LocalDateTime.now()
    }

}
