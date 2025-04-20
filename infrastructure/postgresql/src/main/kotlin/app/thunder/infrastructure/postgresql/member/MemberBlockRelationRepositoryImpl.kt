package app.thunder.infrastructure.postgresql.member

import app.thunder.core.member.repository.MemberBlockRelationRepository
import app.thunder.infrastructure.postgresql.member.entity.MemberBlockRelationEntity
import app.thunder.infrastructure.postgresql.member.persistence.MemberBlockRelationJpaRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
internal class MemberBlockRelationRepositoryImpl(
    private val memberBlockRelationJpaRepository: MemberBlockRelationJpaRepository,
) : MemberBlockRelationRepository {

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
