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
                )
            }
    }

    @Transactional
    override fun verify(mobileVerificationId: Long) {
        mobileVerificationJpaRepository.findById(mobileVerificationId)
            .ifPresent { entity -> entity.verified() }
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
