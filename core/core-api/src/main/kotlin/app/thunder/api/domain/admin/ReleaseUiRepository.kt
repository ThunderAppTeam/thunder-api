package app.thunder.api.domain.admin

import org.springframework.data.jpa.repository.JpaRepository

interface ReleaseUiRepository : JpaRepository<ReleaseUiEntity, Long> {
    fun findByMobileOsAndAppVersion(mobileOs: MobileOs, appVersion: String): ReleaseUiEntity?
}