package app.thunder.api.controller.response

data class GetMemberSettingsResponse(
    val reviewCompleteNotify: Boolean,
    val reviewRequestNotify: Boolean,
    val marketingAgreement: Boolean,
)
