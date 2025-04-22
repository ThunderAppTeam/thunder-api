package app.thunder.core.member.domain

import java.time.LocalDateTime

class MobileVerification(
    val mobileVerificationId: Long,
    val verificationCode: String,
    val expiredAt: LocalDateTime,
    val failedAttemptCount: Int,
) {
    fun isExpired(now: LocalDateTime = LocalDateTime.now()): Boolean {
        return !now.isBefore(expiredAt)
    }

    fun isLocked(): Boolean {
        return failedAttemptCount >= MAX_FAILED_ATTEMPTS
    }

    companion object {
        const val MAX_FAILED_ATTEMPTS = 5
    }
}
