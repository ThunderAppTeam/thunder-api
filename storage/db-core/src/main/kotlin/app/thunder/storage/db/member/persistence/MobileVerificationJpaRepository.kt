package app.thunder.storage.db.member.persistence

import app.thunder.storage.db.member.entity.MobileVerificationEntity
import com.linecorp.kotlinjdsl.support.spring.data.jpa.repository.KotlinJdslJpqlExecutor
import java.time.LocalDateTime
import org.springframework.data.jpa.repository.JpaRepository

internal interface MobileVerificationJpaRepository : JpaRepository<MobileVerificationEntity, Long>, KotlinJdslJpqlExecutor

internal fun MobileVerificationJpaRepository.findAllByMobileNumber(mobileNumber: String): List<MobileVerificationEntity> {
    return this.findAll {
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

internal fun MobileVerificationJpaRepository.findAllByDeviceIdAndNotVerify(deviceId: String): List<MobileVerificationEntity> {
    return this.findAll {
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

internal fun MobileVerificationJpaRepository.findAllByDeviceIdAndCreatedAtAfter(
    deviceId: String,
    createdAt: LocalDateTime,
): List<MobileVerificationEntity> {
    return this.findAll(limit = 5) {
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

internal fun MobileVerificationJpaRepository.findLastByDeviceIdAndMobileNumber(
    deviceId: String,
    mobileNumber: String,
): MobileVerificationEntity? {
    return this.findAll(limit = 1) {
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
