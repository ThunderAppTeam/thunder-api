package app.thunder.core.member.repository

interface MemberBlockRelationRepository {
    fun create(memberId: Long, blockedMemberId: Long, createdBy: Long)
    fun getBlockedMemberIdsByMemberId(memberId: Long): Set<Long>
    fun deleteAllByMemberId(memberId: Long)
}
