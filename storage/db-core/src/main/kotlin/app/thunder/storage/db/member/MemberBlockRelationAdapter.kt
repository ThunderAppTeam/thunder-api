package app.thunder.storage.db.member

import app.thunder.domain.member.MemberBlockRelationPort
import app.thunder.storage.db.member.entity.MemberBlockRelationEntity
import app.thunder.storage.db.member.persistence.MemberBlockRelationJpaRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
internal class MemberBlockRelationAdapter(
    private val memberBlockRelationJpaRepository: MemberBlockRelationJpaRepository,
) : MemberBlockRelationPort {

    @Transactional
    override fun create(memberId: Long, blockedMemberId: Long, createdBy: Long) {
        if (memberBlockRelationJpaRepository.existsByMemberIdAndBlockedMemberId(memberId, blockedMemberId)) {
            return
        }

        val entity = MemberBlockRelationEntity.create(
            memberId = memberId,
            blockedMemberId = blockedMemberId,
            createdBy = createdBy,
        )
        memberBlockRelationJpaRepository.save(entity)
    }

    @Transactional(readOnly = true)
    override fun getBlockedMemberIdsByMemberId(memberId: Long): Set<Long> {
        return memberBlockRelationJpaRepository.findAllByMemberId(memberId)
            .map { it.blockedMemberId }
            .toSet()
    }

    @Transactional
    override fun deleteAllByMemberId(memberId: Long) {
        val ids = memberBlockRelationJpaRepository.findAllByMemberId(memberId)
            .map { it.blockedMemberId }
        memberBlockRelationJpaRepository.deleteAllByIdInBatch(ids)
    }

}
