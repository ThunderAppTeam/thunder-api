package app.thunder.core.member.repository

import app.thunder.core.member.domain.MemberDeletionReason
import java.util.UUID

interface DeletedMemberRepository {
    fun create(
        memberId: Long,
        memberUuid: UUID,
        nickname: String,
        mobileNumber: String,
        deletionReason: MemberDeletionReason,
        otherReason: String?,
    )
}
