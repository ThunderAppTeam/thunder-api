package app.thunder.storage.db.member.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Table(name = "mobile_verification")
@Entity
internal class MobileVerificationEntity private constructor(
    deviceId: String,
    mobileNumber: String,
    mobileCountry: String,
    verificationCode: String,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mobile_verification_id")
    val mobileVerificationId: Long = 0

    @Column(name = "device_id")
    val deviceId: String = deviceId

    @Column(name = "mobile_number")
    val mobileNumber: String = mobileNumber

    @Column(name = "mobile_country")
    val mobileCountry: String = mobileCountry

    @Column(name = "verification_code")
    val verificationCode: String = verificationCode

    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now()

    @Column(name = "expired_at")
    val expiredAt: LocalDateTime = createdAt.plusMinutes(3)

    @Column(name = "verified_at")
    var verifiedAt: LocalDateTime? = null
        protected set


    companion object {
        fun create(
            deviceId: String,
            mobileNumber: String,
            mobileCountry: String,
            verificationCode: String,
        ): MobileVerificationEntity {
            return MobileVerificationEntity(
                deviceId = deviceId,
                mobileNumber = mobileNumber,
                mobileCountry = mobileCountry,
                verificationCode = verificationCode,
            )
        }

        private val RESET_DATE_TIME = LocalDateTime.of(1, 1, 1, 0, 0)
    }

    fun isExpired(): Boolean {
        return LocalDateTime.now().isAfter(expiredAt)
    }

    fun verified() {
        this.verifiedAt = LocalDateTime.now()
    }

    fun reset() {
        this.verifiedAt = RESET_DATE_TIME
    }

}
