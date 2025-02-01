package app.thunder.api.domain.member

import org.springframework.data.jpa.repository.JpaRepository

interface MemberBlockRelationRepository : JpaRepository<MemberBlockRelationEntity, Long> {
    fun findAllByMemberId(memberId: Long): List<MemberBlockRelationEntity>
    fun existsByMemberIdAndBlockedMemberId(memberId: Long, blockedMemberId: Long): Boolean
}