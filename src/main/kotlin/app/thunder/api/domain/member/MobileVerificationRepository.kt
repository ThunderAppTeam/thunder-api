package app.thunder.api.domain.member

import org.springframework.data.jpa.repository.JpaRepository

interface MobileVerificationRepository : JpaRepository<MobileVerificationEntity, Long> {
    fun findAllByMobileNumber(mobileNumber: String): List<MobileVerificationEntity>
}