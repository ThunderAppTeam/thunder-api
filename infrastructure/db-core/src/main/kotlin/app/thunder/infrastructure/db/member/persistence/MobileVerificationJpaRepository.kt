package app.thunder.infrastructure.db.member.persistence

import app.thunder.infrastructure.db.member.entity.MobileVerificationEntity
import com.linecorp.kotlinjdsl.support.spring.data.jpa.repository.KotlinJdslJpqlExecutor
import java.time.LocalDateTime
import org.springframework.data.jpa.repository.JpaRepository

internal interface MobileVerificationJpaRepository : JpaRepository<MobileVerificationEntity, Long>, KotlinJdslJpqlExecutor
{
    fun findAllByMobileNumber(mobileNumber: String): List<MobileVerificationEntity> {
        return findAll {
            select(
                entity(MobileVerificationEntity::class),
            ).from(
                entity(MobileVerificationEntity::class),
            ).whereAnd(
                path(MobileVerificationEntity::mobileNumber).eq(mobileNumber),
                path(MobileVerificationEntity::verifiedAt).isNull(),
            )
        }.filterNotNull()
    }

    fun findAllByDeviceIdAndNotVerify(deviceId: String): List<MobileVerificationEntity> {
        return findAll {
            select(
                entity(MobileVerificationEntity::class),
            ).from(
                entity(MobileVerificationEntity::class),
            ).whereAnd(
                path(MobileVerificationEntity::deviceId).eq(deviceId),
                path(MobileVerificationEntity::verifiedAt).isNull(),
            )
        }.filterNotNull()
    }

    fun findAllByDeviceIdAndCreatedAtAfter(
        deviceId: String,
        createdAt: LocalDateTime,
    ): List<MobileVerificationEntity> {
        return findAll(limit = 5) {
            select(
                entity(MobileVerificationEntity::class),
            ).from(
                entity(MobileVerificationEntity::class),
            ).whereAnd(
                path(MobileVerificationEntity::deviceId).eq(deviceId),
                path(MobileVerificationEntity::createdAt).ge(createdAt),
                path(MobileVerificationEntity::verifiedAt).isNull(),
            )
        }.filterNotNull()
    }

    fun findLastByDeviceIdAndMobileNumber(
        deviceId: String,
        mobileNumber: String,
    ): MobileVerificationEntity? {
        return findAll(limit = 1) {
            select(
                entity(MobileVerificationEntity::class),
            ).from(
                entity(MobileVerificationEntity::class),
            ).whereAnd(
                path(MobileVerificationEntity::deviceId).eq(deviceId),
                path(MobileVerificationEntity::mobileNumber).eq(mobileNumber),
                path(MobileVerificationEntity::verifiedAt).isNull(),
            ).orderBy(
                path(MobileVerificationEntity::mobileVerificationId).desc(),
            )
        }.firstOrNull()
    }
}
