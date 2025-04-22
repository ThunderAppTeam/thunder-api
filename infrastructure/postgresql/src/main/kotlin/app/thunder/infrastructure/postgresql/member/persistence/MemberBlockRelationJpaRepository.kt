package app.thunder.infrastructure.postgresql.member.persistence

import app.thunder.infrastructure.postgresql.member.entity.MemberBlockRelationEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

internal interface MemberBlockRelationJpaRepository : JpaRepository<MemberBlockRelationEntity, Long> {
    fun findAllByMemberId(memberId: Long): List<MemberBlockRelationEntity>
    fun existsByMemberIdAndBlockedMemberId(memberId: Long, blockedMemberId: Long): Boolean

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM MemberBlockRelationEntity r WHERE r.memberId = :memberId OR r.blockedMemberId = :memberId")
    fun deleteAllReferencingMember(@Param("memberId") memberId: Long)
}
