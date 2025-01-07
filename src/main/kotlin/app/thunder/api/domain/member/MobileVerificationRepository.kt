package app.thunder.api.domain.member

import com.linecorp.kotlinjdsl.support.spring.data.jpa.repository.KotlinJdslJpqlExecutor
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime

interface MobileVerificationRepository : JpaRepository<MobileVerificationEntity, Long>, KotlinJdslJpqlExecutor

fun MobileVerificationRepository.findAllByDeviceIdAndCreatedAtAfter(
    deviceId: String,
    createdAt: LocalDateTime
): List<MobileVerificationEntity?> {
    return this.findAll(limit = 5) {
        select(
            entity(MobileVerificationEntity::class)
        ).from(
            entity(MobileVerificationEntity::class)
        ).whereAnd(
            path(MobileVerificationEntity::deviceId).eq(deviceId),
            path(MobileVerificationEntity::createdAt).ge(createdAt),
        )
    }
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