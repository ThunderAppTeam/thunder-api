package app.thunder.storage.db.member.persistence

import app.thunder.storage.db.member.entity.MemberBlockRelationEntity
import org.springframework.data.jpa.repository.JpaRepository

internal interface MemberBlockRelationJpaRepository : JpaRepository<MemberBlockRelationEntity, Long> {
    fun findAllByMemberId(memberId: Long): List<MemberBlockRelationEntity>
    fun existsByMemberIdAndBlockedMemberId(memberId: Long, blockedMemberId: Long): Boolean
}
