package app.thunder.domain.member

interface MemberBlockRelationPort {
    fun create(memberId: Long, blockedMemberId: Long, createdBy: Long)
    fun getBlockedMemberIdsByMemberId(memberId: Long): Set<Long>
    fun deleteAllByMemberId(memberId: Long)
}
