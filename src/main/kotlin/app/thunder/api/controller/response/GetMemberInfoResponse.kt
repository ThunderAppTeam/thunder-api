package app.thunder.api.controller.response

import java.time.ZonedDateTime
import java.util.UUID

data class GetMemberInfoResponse(
    val memberUuid: UUID,
    val nickname: String,
    val mobileNumber: String,
    val registeredAt: ZonedDateTime,
)
