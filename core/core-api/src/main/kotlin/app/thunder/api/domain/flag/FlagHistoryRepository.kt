package app.thunder.api.domain.flag

import org.springframework.data.jpa.repository.JpaRepository

interface FlagHistoryRepository : JpaRepository<FlagHistoryEntity, Long> {
    fun existsByMemberIdAndBodyPhotoId(memberId: Long, bodyPhotoId: Long): Boolean
    fun findAllByMemberId(memberId: Long): List<FlagHistoryEntity>
}