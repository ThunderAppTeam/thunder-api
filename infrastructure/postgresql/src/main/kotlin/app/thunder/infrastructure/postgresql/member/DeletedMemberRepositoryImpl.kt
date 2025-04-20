package app.thunder.infrastructure.postgresql.member

import app.thunder.core.member.repository.DeletedMemberRepository
import app.thunder.core.member.domain.MemberDeletionReason
import app.thunder.infrastructure.postgresql.member.entity.DeletedMemberEntity
import app.thunder.infrastructure.postgresql.member.persistence.DeletedMemberJpaRepository
import java.util.UUID
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
internal class DeletedMemberRepositoryImpl(
    private val deletedMemberJpaRepository: DeletedMemberJpaRepository,
) : DeletedMemberRepository {

    @Transactional
    override fun create(
        memberId: Long,
        memberUuid: UUID,
        nickname: String,
        mobileNumber: String,
        deletionReason: MemberDeletionReason,
        otherReason: String?,
    ) {
        val encodedMobileNumber = BCryptPasswordEncoder().encode(mobileNumber)
        val deletedMemberEntity = DeletedMemberEntity.create(
            memberId = memberId,
            memberUuid = memberUuid,
            nickname = nickname,
            mobileNumber = encodedMobileNumber,
            deletionReason = deletionReason,
            otherReason = otherReason,
        )
        deletedMemberJpaRepository.save(deletedMemberEntity)
    }

}
