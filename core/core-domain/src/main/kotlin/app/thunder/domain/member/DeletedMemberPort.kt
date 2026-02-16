package app.thunder.domain.member

import java.util.UUID

interface DeletedMemberPort {
    fun create(
        memberId: Long,
        memberUuid: UUID,
        nickname: String,
        mobileNumber: String,
        deletionReason: MemberDeletionReason,
        otherReason: String?,
    )
}
