package app.thunder.api.application.dto

data class MemberAccessToken(
    val member: app.thunder.api.domain.member.Member?,
    val accessToken: String?,
)
