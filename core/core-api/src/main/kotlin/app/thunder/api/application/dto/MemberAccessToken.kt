package app.thunder.api.application.dto

import app.thunder.domain.member.Member

data class MemberAccessToken(
    val member: Member?,
    val accessToken: String?,
)
