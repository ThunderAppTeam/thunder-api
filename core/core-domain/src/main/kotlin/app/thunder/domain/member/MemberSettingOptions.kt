package app.thunder.domain.member

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class MemberSettingOptions(
    val reviewCompleteNotify: Boolean,
    val reviewRequestNotify: Boolean,
    val marketingAgreement: Boolean,
)
