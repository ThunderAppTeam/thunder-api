package app.thunder.api.controller.request

import app.thunder.core.admin.domain.MobileOs
import jakarta.validation.constraints.NotNull

data class PostReleaseUiRequest(
    @field:NotNull
    val mobileOs: MobileOs,

    @field:NotNull
    val appVersion: String,

    @field:NotNull
    val isRelease: Boolean,
)
