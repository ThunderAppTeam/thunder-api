package app.thunder.api.domain.member.repository

import app.thunder.api.domain.member.entity.MobileVerificationEntity
import com.linecorp.kotlinjdsl.support.spring.data.jpa.repository.KotlinJdslJpqlExecutor
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime

interface MobileVerificationRepository : JpaRepository<MobileVerificationEntity, Long>, KotlinJdslJpqlExecutor

fun MobileVerificationRepository.findAllByMobileNumber(mobileNumber: String): List<MobileVerificationEntity> {
    return this.findAll {
        select(
            entity(MobileVerificationEntity::class)
        ).from(
            entity(MobileVerificationEntity::class)
        ).whereAnd(
            path(MobileVerificationEntity::mobileNumber).eq(mobileNumber),
            path(MobileVerificationEntity::verifiedAt).isNull(),
        )
    }.filterNotNull()
}

fun MobileVerificationRepository.findAllByDeviceIdAndNotVerify(deviceId: String): List<MobileVerificationEntity> {
    return this.findAll {
        select(
            entity(MobileVerificationEntity::class)
        ).from(
            entity(MobileVerificationEntity::class)
        ).whereAnd(
            path(MobileVerificationEntity::deviceId).eq(deviceId),
            path(MobileVerificationEntity::verifiedAt).isNull(),
        )
    }.filterNotNull()
}

fun MobileVerificationRepository.findAllByDeviceIdAndCreatedAtAfter(
    deviceId: String,
    createdAt: LocalDateTime
): List<MobileVerificationEntity> {
    return this.findAll(limit = 5) {
        select(
            entity(MobileVerificationEntity::class)
        ).from(
            entity(MobileVerificationEntity::class)
        ).whereAnd(
            path(MobileVerificationEntity::deviceId).eq(deviceId),
            path(MobileVerificationEntity::createdAt).ge(createdAt),
            path(MobileVerificationEntity::verifiedAt).isNull(),
        )
    }.filterNotNull()
}

fun MobileVerificationRepository.findLastByDeviceIdAndMobileNumber(deviceId: String,
                                                                   mobileNumber: String): MobileVerificationEntity? {
    return this.findAll(limit = 1) {
        select(
            entity(MobileVerificationEntity::class)
        ).from(
            entity(MobileVerificationEntity::class)
        ).whereAnd(
            path(MobileVerificationEntity::deviceId).eq(deviceId),
            path(MobileVerificationEntity::mobileNumber).eq(mobileNumber),
            path(MobileVerificationEntity::verifiedAt).isNull(),
        ).orderBy(
            path(MobileVerificationEntity::mobileVerificationId).desc()
        )
    }.firstOrNull()
}