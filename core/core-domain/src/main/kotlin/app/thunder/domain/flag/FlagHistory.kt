package app.thunder.domain.flag

import java.time.LocalDateTime

data class FlagHistory(
    val flagHistoryId: Long,
    val memberId: Long,
    val bodyPhotoId: Long,
    val flagReason: FlagReason,
    val otherReason: String?,
    val createdAt: LocalDateTime,
)
