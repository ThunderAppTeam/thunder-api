package app.thunder.infrastructure.postgresql.admin

import app.thunder.core.admin.repository.ReleaseUiRepository
import app.thunder.infrastructure.postgresql.admin.entity.ReleaseUiEntity
import app.thunder.infrastructure.postgresql.admin.entity.ReleaseUiMobileOs
import app.thunder.infrastructure.postgresql.admin.persistence.ReleaseUiJpaRepository
import java.time.LocalDateTime
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
internal class ReleaseUiRepositoryImpl(
    private val releaseUiJpaRepository: ReleaseUiJpaRepository,
) : ReleaseUiRepository {

    @Transactional(readOnly = true)
    override fun getIsRelease(mobileOs: String, appVersion: String): Boolean {
        val mobileOsType = ReleaseUiMobileOs.valueOf(mobileOs)
        return releaseUiJpaRepository.findByMobileOsAndAppVersion(mobileOsType, appVersion)?.isRelease
            ?: false
    }

    @Transactional
    override fun createOrUpdate(mobileOs: String, appVersion: String, isRelease: Boolean, updatedAt: LocalDateTime) {
        val mobileOsType = ReleaseUiMobileOs.valueOf(mobileOs)
        val releaseUiEntity = releaseUiJpaRepository.findByMobileOsAndAppVersion(mobileOsType, appVersion)
            ?: ReleaseUiEntity.create(mobileOsType, appVersion)
        releaseUiEntity.update(isRelease, updatedAt)
        releaseUiJpaRepository.save(releaseUiEntity)
    }

}
