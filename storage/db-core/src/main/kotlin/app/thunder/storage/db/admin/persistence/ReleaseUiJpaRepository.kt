package app.thunder.storage.db.admin.persistence

import app.thunder.storage.db.admin.entity.ReleaseUiEntity
import app.thunder.storage.db.admin.entity.ReleaseUiMobileOs
import org.springframework.data.jpa.repository.JpaRepository

internal interface ReleaseUiJpaRepository : JpaRepository<ReleaseUiEntity, Long> {
    fun findByMobileOsAndAppVersion(mobileOs: ReleaseUiMobileOs, appVersion: String): ReleaseUiEntity?
}
