package app.thunder.storage.db.member

import app.thunder.domain.member.DeletedMemberPort
import app.thunder.domain.member.MemberDeletionReason
import app.thunder.storage.db.member.entity.DeletedMemberEntity
import app.thunder.storage.db.member.persistence.DeletedMemberJpaRepository
import java.util.UUID
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
internal class DeletedMemberAdapter(
    private val deletedMemberJpaRepository: DeletedMemberJpaRepository,
) : DeletedMemberPort {

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
