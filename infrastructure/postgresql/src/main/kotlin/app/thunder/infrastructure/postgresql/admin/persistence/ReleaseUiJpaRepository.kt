package app.thunder.infrastructure.postgresql.admin.persistence

import app.thunder.infrastructure.postgresql.admin.entity.ReleaseUiEntity
import app.thunder.infrastructure.postgresql.admin.entity.ReleaseUiMobileOs
import org.springframework.data.jpa.repository.JpaRepository

internal interface ReleaseUiJpaRepository : JpaRepository<ReleaseUiEntity, Long> {
    fun findByMobileOsAndAppVersion(mobileOs: ReleaseUiMobileOs, appVersion: String): ReleaseUiEntity?
}
