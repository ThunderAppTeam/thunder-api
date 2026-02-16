package app.thunder.domain.admin

import java.time.LocalDateTime

interface ReleaseUiPort {
    fun getIsRelease(mobileOs: String, appVersion: String): Boolean
    fun createOrUpdate(mobileOs: String, appVersion: String, isRelease: Boolean, updatedAt: LocalDateTime)
}
