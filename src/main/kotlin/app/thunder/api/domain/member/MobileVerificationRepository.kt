package app.thunder.api.domain.member

import com.linecorp.kotlinjdsl.support.spring.data.jpa.repository.KotlinJdslJpqlExecutor
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

interface MobileVerificationRepository : JpaRepository<MobileVerificationEntity, Long>, KotlinJdslJpqlExecutor {

    @Transactional(readOnly = true)
    fun findAllByMobileNumberAndCreatedAtAfter(
        mobileNumber: String,
        createdAt: LocalDateTime
    ): List<MobileVerificationEntity?> {
        return this.findAll {
            select(
                entity(MobileVerificationEntity::class)
            ).from(
                entity(MobileVerificationEntity::class)
            ).whereAnd(
                path(MobileVerificationEntity::mobileNumber).eq(mobileNumber),
                path(MobileVerificationEntity::createdAt).ge(createdAt),
            )
        }
    }

    @Transactional(readOnly = true)
    fun findLastByMobileNumberAndVerifiedAtIsNull(mobileNumber: String): MobileVerificationEntity? {
        return this.findAll(limit = 1) {
            select(
                entity(MobileVerificationEntity::class)
            ).from(
                entity(MobileVerificationEntity::class)
            ).whereAnd(
                path(MobileVerificationEntity::mobileNumber).eq(mobileNumber),
                path(MobileVerificationEntity::verifiedAt).isNull(),
            ).orderBy(
                path(MobileVerificationEntity::mobileVerificationId).desc()
            )
        }.firstOrNull()
    }

}