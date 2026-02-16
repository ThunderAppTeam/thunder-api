package app.thunder.storage.db.admin

import app.thunder.domain.admin.ReleaseUiPort
import app.thunder.storage.db.admin.entity.ReleaseUiEntity
import app.thunder.storage.db.admin.entity.ReleaseUiMobileOs
import app.thunder.storage.db.admin.persistence.ReleaseUiJpaRepository
import java.time.LocalDateTime
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
internal class ReleaseUiAdapter(
    private val releaseUiJpaRepository: ReleaseUiJpaRepository,
) : ReleaseUiPort {

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
