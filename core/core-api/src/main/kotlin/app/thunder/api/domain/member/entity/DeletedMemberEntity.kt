package app.thunder.api.domain.member.entity

import app.thunder.api.domain.member.MemberDeletionReason
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.UUID

@Table(name = "deleted_member")
@Entity
class DeletedMemberEntity private constructor(
    memberId: Long,
    memberUuid: UUID,
    nickname: String,
    mobileNumber: String,
    deletionReason: MemberDeletionReason,
    otherReason: String?,
) {
    @Id
    @Column(name = "member_id")
    val memberId: Long = memberId

    @Column(name = "member_uuid")
    val memberUuid: UUID = memberUuid

    @Column(name = "nickname")
    val nickname: String = nickname

    @Column(name = "mobile_number")
    val mobileNumber: String = mobileNumber

    @Enumerated(EnumType.STRING)
    @Column(name = "deletion_reason")
    val deletionReason: MemberDeletionReason = deletionReason

    @Column(name = "other_reason")
    val otherReason: String? = otherReason

    @Column(name = "deleted_at")
    val deletedAt: LocalDateTime = LocalDateTime.now()


    companion object {
        fun create(
            memberId: Long,
            memberUuid: UUID,
            nickname: String,
            mobileNumber: String,
            deletionReason: MemberDeletionReason,
            otherReason: String?
        ): DeletedMemberEntity {
            return DeletedMemberEntity(
                memberId = memberId,
                memberUuid = memberUuid,
                nickname = nickname,
                mobileNumber = mobileNumber,
                deletionReason = deletionReason,
                otherReason = otherReason,
            )
        }
    }

}
