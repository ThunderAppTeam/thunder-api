package app.thunder.api.application

import app.thunder.core.admin.domain.MobileOs
import app.thunder.core.admin.repository.ReleaseUiRepository
import java.time.LocalDateTime
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AdminService(
    private val releaseUiRepository: ReleaseUiRepository,
) {

    @Transactional
    fun getReleaseUi(mobileOs: MobileOs, appVersion: String): Boolean {
        return releaseUiRepository.getIsRelease(mobileOs = mobileOs.name, appVersion = appVersion)
    }

    @Transactional
    fun createOrUpdateReleaseUi(mobileOs: MobileOs, appVersion: String, isRelease: Boolean) {
        releaseUiRepository.createOrUpdate(mobileOs = mobileOs.name,
                                     appVersion = appVersion,
                                     isRelease = isRelease,
                                     updatedAt = LocalDateTime.now())
    }

}
