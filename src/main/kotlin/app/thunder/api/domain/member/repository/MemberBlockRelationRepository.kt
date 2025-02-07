package app.thunder.api.domain.member.repository

import app.thunder.api.domain.member.entity.MemberBlockRelationEntity
import org.springframework.data.jpa.repository.JpaRepository

interface MemberBlockRelationRepository : JpaRepository<MemberBlockRelationEntity, Long> {
    fun findAllByMemberId(memberId: Long): List<MemberBlockRelationEntity>
    fun existsByMemberIdAndBlockedMemberId(memberId: Long, blockedMemberId: Long): Boolean
}