package app.thunder.api.domain.member

import java.time.LocalDateTime

data class MemberBlockRelation private constructor(
    val memberBlockRelationId: Long,
    val memberId: Long,
    val blockedMemberId: Long,
    val createdAt: LocalDateTime,
    val createdBy: Long,
) {

    companion object {
        fun from(entity: MemberBlockRelationEntity): MemberBlockRelation {
            return MemberBlockRelation(
                memberBlockRelationId = entity.memberBlockRelationId,
                memberId = entity.memberId,
                blockedMemberId = entity.blockedMemberId,
                createdAt = entity.createdAt,
                createdBy = entity.createdBy,
            )
        }
    }

}
