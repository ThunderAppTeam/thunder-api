package app.thunder.core.flag.repository

import app.thunder.core.flag.domain.FlagHistory
import app.thunder.core.flag.domain.FlagReason

interface FlagHistoryRepository {
    fun create(memberId: Long, bodyPhotoId: Long, flagReason: FlagReason, otherReason: String?)

    fun exists(memberId: Long, bodyPhotoId: Long): Boolean

    fun getAll(): List<FlagHistory>

    fun getAllByMemberId(memberId: Long): List<FlagHistory>

    fun getFlagCountByBodyPhotoId(): Map<Long, Int>
}
