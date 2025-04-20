package app.thunder.core.admin.repository

import java.time.LocalDateTime

interface ReleaseUiRepository {
    fun getIsRelease(mobileOs: String, appVersion: String): Boolean
    fun createOrUpdate(mobileOs: String, appVersion: String, isRelease: Boolean, updatedAt: LocalDateTime)
}
