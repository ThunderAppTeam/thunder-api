package app.thunder.api.application

import app.thunder.domain.admin.MobileOs
import app.thunder.domain.admin.ReleaseUiPort
import java.time.LocalDateTime
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AdminService(
    private val releaseUiPort: ReleaseUiPort,
) {

    @Transactional
    fun getReleaseUi(mobileOs: MobileOs, appVersion: String): Boolean {
        return releaseUiPort.getIsRelease(mobileOs = mobileOs.name, appVersion = appVersion)
    }

    @Transactional
    fun createOrUpdateReleaseUi(mobileOs: MobileOs, appVersion: String, isRelease: Boolean) {
        releaseUiPort.createOrUpdate(mobileOs = mobileOs.name,
                                     appVersion = appVersion,
                                     isRelease = isRelease,
                                     updatedAt = LocalDateTime.now())
    }

}
