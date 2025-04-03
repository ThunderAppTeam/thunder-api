package app.thunder.api.domain.admin

import jakarta.persistence.*
import java.time.LocalDateTime

@Table(name = "release_ui")
@Entity
class ReleaseUiEntity private constructor(
    mobileOs: MobileOs,
    appVersion: String,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "release_ui_id")
    val releaseUiId: Long = 0

    @Enumerated(EnumType.STRING)
    @Column(name = "mobile_os", nullable = false)
    val mobileOs: MobileOs = mobileOs

    @Column(name = "app_version", nullable = false)
    val appVersion: String = appVersion

    @Column(name = "is_release", nullable = false)
    var isRelease: Boolean = false
        protected set

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime? = null
        protected set


    companion object {
        fun create(mobileOs: MobileOs, appVersion: String): ReleaseUiEntity =
            ReleaseUiEntity(mobileOs, appVersion)
    }

    fun update(isRelease: Boolean, updatedAt: LocalDateTime) {
        this.isRelease = isRelease
        this.updatedAt = updatedAt
    }

}
