package app.thunder.api.domain.member.adapter

import app.thunder.api.domain.member.MemberDeletionReason
import app.thunder.api.domain.member.entity.DeletedMemberEntity
import app.thunder.api.domain.member.repository.DeletedMemberRepository
import java.util.UUID
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class DeletedMemberAdapter(
    private val deletedMemberRepository: DeletedMemberRepository,
) {

    @Transactional
    fun create(
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
        deletedMemberRepository.save(deletedMemberEntity)
    }

}