package app.thunder.core.member.domain

import java.time.LocalDateTime

class MobileVerification(
    val mobileVerificationId: Long,
    val verificationCode: String,
    val expiredAt: LocalDateTime,
) {
    fun isExpired(): Boolean {
        return LocalDateTime.now().isAfter(expiredAt)
    }
}
