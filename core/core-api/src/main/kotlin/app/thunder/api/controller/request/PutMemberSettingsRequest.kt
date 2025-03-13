package app.thunder.api.controller.request

data class PutMemberSettingsRequest(
    val reviewCompleteNotify: Boolean,
    val reviewRequestNotify: Boolean,
    val marketingAgreement: Boolean,
)
