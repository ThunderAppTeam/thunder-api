package app.thunder.infrastructure.postgresql.flag.persistence

import app.thunder.infrastructure.postgresql.flag.entity.FlagHistoryEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

internal interface FlagHistoryJpaRepository : JpaRepository<FlagHistoryEntity, Long> {
    fun existsByMemberIdAndBodyPhotoId(memberId: Long, bodyPhotoId: Long): Boolean
    fun findAllByMemberId(memberId: Long): List<FlagHistoryEntity>

    @Modifying
    @Query("DELETE FROM FlagHistoryEntity f WHERE f.bodyPhotoId = :bodyPhotoId")
    fun deleteAllByBodyPhotoId(@Param("bodyPhotoId") bodyPhotoId: Long)
}
