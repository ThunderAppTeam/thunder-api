package app.thunder.api.domain.member.adapter

import app.thunder.api.domain.member.repository.MemberBlockRelationRepository
import app.thunder.api.domain.member.entity.MemberBlockRelationEntity
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class MemberBlockRelationAdapter(
    private val memberBlockRelationRepository: MemberBlockRelationRepository
) {

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

    @Transactional
    fun deleteAllByMemberId(memberId:Long) {
        val ids = memberBlockRelationRepository.findAllByMemberId(memberId)
            .map { it.blockedMemberId }
        memberBlockRelationRepository.deleteAllByIdInBatch(ids)
    }

}