package app.thunder.api.application

import app.thunder.api.domain.admin.MobileOs
import app.thunder.api.domain.admin.ReleaseUiEntity
import app.thunder.api.domain.admin.ReleaseUiRepository
import java.time.LocalDateTime
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AdminService(
    private val releaseUiRepository: ReleaseUiRepository,
) {

    @Transactional
    fun getReleaseUi(mobileOs: MobileOs, appVersion: String): Boolean {
        return releaseUiRepository.findByMobileOsAndAppVersion(mobileOs, appVersion)?.isRelease
            ?: false
    }

    @Transactional
    fun createOrUpdateReleaseUi(mobileOs: MobileOs, appVersion: String, isRelease: Boolean) {
        val releaseUiEntity = releaseUiRepository.findByMobileOsAndAppVersion(mobileOs, appVersion)
            ?: ReleaseUiEntity.create(mobileOs, appVersion)
        releaseUiEntity.update(isRelease, LocalDateTime.now())
        releaseUiRepository.save(releaseUiEntity)
    }

}