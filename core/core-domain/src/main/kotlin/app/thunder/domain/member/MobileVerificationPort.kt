package app.thunder.domain.member

import java.time.LocalDateTime

interface MobileVerificationPort {
    fun getCountByDeviceIdAndCreatedAtAfter(deviceId: String, createdAt: LocalDateTime): Int
    fun create(deviceId: String, mobileNumber: String, mobileCountry: String, verificationCode: String)
    fun getLastByDeviceIdAndMobileNumber(deviceId: String, mobileNumber: String): MobileVerification?
    fun verify(mobileVerificationId: Long)
    fun resetByDeviceId(deviceId: String)
    fun resetByMobileNumber(mobileNumber: String)
}
