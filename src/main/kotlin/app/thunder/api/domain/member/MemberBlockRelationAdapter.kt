package app.thunder.api.domain.member

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class MemberBlockRelationAdapter(
    private val memberBlockRelationRepository: MemberBlockRelationRepository
) {

    @Transactional(readOnly = true)
    fun getByMemberId(memberId: Long): List<MemberBlockRelation> {
        return memberBlockRelationRepository.findAllByMemberId(memberId)
            .map(MemberBlockRelation::from)
    }

    @Transactional
    fun create(memberId: Long, blockedMemberId: Long, createdBy: Long) {
        if (memberBlockRelationRepository.existsByMemberIdAndBlockedMemberId(memberId, blockedMemberId)) {
            return
        }

        val entity = MemberBlockRelationEntity.create(memberId = memberId,
                                                      blockedMemberId = blockedMemberId,
                                                      createdBy = createdBy)
        memberBlockRelationRepository.save(entity)
    }

}