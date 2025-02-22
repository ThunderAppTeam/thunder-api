package app.thunder.api.controller.response

data class GetMemberSettingsResponse(
    val memberId: Long,
    val reviewCompleteNotify: Boolean,
    val reviewRequestNotify: Boolean,
    val marketingAgreement: Boolean,
)
