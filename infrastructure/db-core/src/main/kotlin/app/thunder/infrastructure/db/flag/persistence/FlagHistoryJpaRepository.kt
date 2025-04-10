package app.thunder.infrastructure.db.flag.persistence

import app.thunder.infrastructure.db.flag.entity.FlagHistoryEntity
import org.springframework.data.jpa.repository.JpaRepository

internal interface FlagHistoryJpaRepository : JpaRepository<FlagHistoryEntity, Long> {
    fun existsByMemberIdAndBodyPhotoId(memberId: Long, bodyPhotoId: Long): Boolean
    fun findAllByMemberId(memberId: Long): List<FlagHistoryEntity>
}
