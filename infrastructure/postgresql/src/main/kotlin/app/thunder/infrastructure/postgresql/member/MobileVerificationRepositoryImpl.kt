package app.thunder.infrastructure.postgresql.member

import app.thunder.core.member.domain.MobileVerification
import app.thunder.core.member.repository.MobileVerificationRepository
import app.thunder.infrastructure.postgresql.member.entity.MobileVerificationEntity
import app.thunder.infrastructure.postgresql.member.persistence.MobileVerificationJpaRepository
import java.time.LocalDateTime
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
internal class MobileVerificationRepositoryImpl(
    private val mobileVerificationJpaRepository: MobileVerificationJpaRepository,
) : MobileVerificationRepository {

    @Transactional(readOnly = true)
    override fun getCountByDeviceIdAndCreatedAtAfter(deviceId: String, createdAt: LocalDateTime): Int {
        return mobileVerificationJpaRepository.findAllByDeviceIdAndCreatedAtAfter(deviceId, createdAt)
            .size
    }

    @Transactional(readOnly = true)
    override fun getCountByMobileNumberAndCreatedAtAfter(mobileNumber: String, createdAt: LocalDateTime): Int {
        return mobileVerificationJpaRepository
            .countByMobileNumberAndCreatedAtAfterAndVerifiedAtIsNull(mobileNumber, createdAt)
    }

    @Transactional
    override fun lockSendLimit(deviceId: String, mobileNumber: String) {
        mobileVerificationJpaRepository.lockByKey("mobile_verification:device:$deviceId")
        mobileVerificationJpaRepository.lockByKey("mobile_verification:mobile:$mobileNumber")
    }

    @Transactional
    override fun create(deviceId: String, mobileNumber: String, mobileCountry: String, verificationCode: String) {
        val entity = MobileVerificationEntity.create(deviceId, mobileNumber, mobileCountry, verificationCode)
        mobileVerificationJpaRepository.save(entity)
    }

    @Transactional(readOnly = true)
    override fun getLastByDeviceIdAndMobileNumber(deviceId: String, mobileNumber: String): MobileVerification? {
        return mobileVerificationJpaRepository.findLastByDeviceIdAndMobileNumber(deviceId, mobileNumber)
            ?.let { entity ->
                MobileVerification(
                    mobileVerificationId = entity.mobileVerificationId,
                    verificationCode = entity.verificationCode,
                    expiredAt = entity.expiredAt,
                    failedAttemptCount = entity.failedAttemptCount,
                )
            }
    }

    @Transactional(readOnly = true)
    override fun existsVerifiedByMobileNumberAfter(mobileNumber: String, verifiedAfter: LocalDateTime): Boolean {
        return mobileVerificationJpaRepository.existsByMobileNumberAndVerifiedAtAfter(mobileNumber, verifiedAfter)
    }

    @Transactional
    override fun verify(mobileVerificationId: Long): Boolean {
        return mobileVerificationJpaRepository.markVerified(
            mobileVerificationId,
            LocalDateTime.now(),
            MobileVerification.MAX_FAILED_ATTEMPTS,
        ) > 0
    }

    @Transactional
    override fun increaseFailedAttemptCount(mobileVerificationId: Long) {
        mobileVerificationJpaRepository.increaseFailedAttemptCount(mobileVerificationId)
    }

    @Transactional
    override fun resetByDeviceId(deviceId: String) {
        mobileVerificationJpaRepository.findAllByDeviceIdAndNotVerify(deviceId)
            .forEach { entity -> entity.reset() }
    }

    @Transactional
    override fun resetByMobileNumber(mobileNumber: String) {
        mobileVerificationJpaRepository.findAllByMobileNumber(mobileNumber)
            .forEach { entity -> entity.reset() }
    }

}
