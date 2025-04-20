package app.thunder.core.member.repository

import app.thunder.core.member.domain.MobileVerification
import java.time.LocalDateTime

interface MobileVerificationRepository {
    fun getCountByDeviceIdAndCreatedAtAfter(deviceId: String, createdAt: LocalDateTime): Int
    fun create(deviceId: String, mobileNumber: String, mobileCountry: String, verificationCode: String)
    fun getLastByDeviceIdAndMobileNumber(deviceId: String, mobileNumber: String): MobileVerification?
    fun verify(mobileVerificationId: Long)
    fun resetByDeviceId(deviceId: String)
    fun resetByMobileNumber(mobileNumber: String)
}
