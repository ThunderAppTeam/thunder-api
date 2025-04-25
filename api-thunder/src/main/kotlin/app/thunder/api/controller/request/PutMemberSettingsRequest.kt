package app.thunder.api.controller.request

import com.fasterxml.jackson.annotation.JsonProperty

data class PutMemberSettingsRequest(
    @JsonProperty(required = true)
    val reviewCompleteNotify: Boolean,
    @JsonProperty(required = true)
    val reviewRequestNotify: Boolean,
    @JsonProperty(required = true)
    val marketingAgreement: Boolean,
)
