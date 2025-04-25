package app.thunder.api.controller.request

import app.thunder.core.admin.domain.MobileOs
import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotNull

data class PostReleaseUiRequest(
    @field:NotNull
    val mobileOs: MobileOs,

    @field:NotNull
    val appVersion: String,

    @field:NotNull
    @JsonProperty(required = true)
    val isRelease: Boolean,
)
