package app.thunder.api.domain.flag

import java.time.LocalDateTime

class FlagHistory private constructor(
    val flagHistoryId: Long,
    val memberId: Long,
    val bodyPhotoId: Long,
    val flagReason: FlagReason,
    val otherReason: String?,
    val createdAt: LocalDateTime,
) {


    companion object {
        fun from(entity: FlagHistoryEntity): FlagHistory {
            return FlagHistory(
                flagHistoryId = entity.flagHistoryId,
                memberId = entity.memberId,
                bodyPhotoId = entity.bodyPhotoId,
                flagReason = entity.flagReason,
                otherReason = entity.otherReason,
                createdAt = entity.createdAt
            )
        }
    }

}
