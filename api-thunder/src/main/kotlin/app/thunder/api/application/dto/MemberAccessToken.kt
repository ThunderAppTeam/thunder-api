package app.thunder.api.application.dto

import app.thunder.core.member.domain.Member

data class MemberAccessToken(
    val member: Member?,
    val accessToken: String?,
)
