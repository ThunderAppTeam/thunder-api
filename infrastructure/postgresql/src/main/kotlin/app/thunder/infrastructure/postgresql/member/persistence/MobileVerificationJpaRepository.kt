package app.thunder.infrastructure.postgresql.member.persistence

import app.thunder.infrastructure.postgresql.member.entity.MobileVerificationEntity
import com.linecorp.kotlinjdsl.support.spring.data.jpa.repository.KotlinJdslJpqlExecutor
import java.time.LocalDateTime
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

internal interface MobileVerificationJpaRepository : JpaRepository<MobileVerificationEntity, Long>, KotlinJdslJpqlExecutor
{
    fun countByMobileNumberAndCreatedAtAfterAndVerifiedAtIsNull(mobileNumber: String, createdAt: LocalDateTime): Int

    fun existsByMobileNumberAndVerifiedAtAfter(mobileNumber: String, verifiedAt: LocalDateTime): Boolean

    @Query(value = "SELECT CAST(pg_advisory_xact_lock(hashtextextended(:key, 0)) AS text)", nativeQuery = true)
    fun lockByKey(@Param("key") key: String): String

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
        """
        UPDATE MobileVerificationEntity e SET e.verifiedAt = :verifiedAt
        WHERE e.mobileVerificationId = :id AND e.verifiedAt IS NULL AND e.failedAttemptCount < :maxFailedAttempts
        """
    )
    fun markVerified(
        @Param("id") id: Long,
        @Param("verifiedAt") verifiedAt: LocalDateTime,
        @Param("maxFailedAttempts") maxFailedAttempts: Int,
    ): Int

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE MobileVerificationEntity e SET e.failedAttemptCount = e.failedAttemptCount + 1 WHERE e.mobileVerificationId = :id")
    fun increaseFailedAttemptCount(@Param("id") id: Long): Int

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
