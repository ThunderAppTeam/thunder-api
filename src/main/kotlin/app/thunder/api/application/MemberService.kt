package app.thunder.api.application

import app.thunder.api.domain.member.Member
import app.thunder.api.domain.member.MemberDeletionReason
import app.thunder.api.domain.member.adapter.DeletedMemberAdapter
import app.thunder.api.domain.member.adapter.MemberAdapter
import app.thunder.api.domain.member.adapter.MemberBlockRelationAdapter
import app.thunder.api.domain.photo.BodyPhotoAdapter
import app.thunder.api.domain.review.adapter.ReviewableBodyPhotoAdapter
import app.thunder.api.func.nullIfBlank
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MemberService(
    private val memberAdapter: MemberAdapter,
    private val memberBlockRelationAdapter: MemberBlockRelationAdapter,
    private val deletedMemberAdapter: DeletedMemberAdapter,
    private val bodyPhotoAdapter: BodyPhotoAdapter,
    private val reviewableBodyPhotoAdapter: ReviewableBodyPhotoAdapter,
) {

    @Transactional(readOnly = true)
    fun getById(memberId: Long): Member {
        return memberAdapter.getById(memberId)
    }

    @Transactional
    fun block(requestMemberId: Long, blockedMemberId: Long) {
        memberBlockRelationAdapter.create(memberId = blockedMemberId,
                                          blockedMemberId = requestMemberId,
                                          createdBy = requestMemberId)
        memberBlockRelationAdapter.create(memberId = requestMemberId,
                                          blockedMemberId = blockedMemberId,
                                          createdBy = requestMemberId)

        reviewableBodyPhotoAdapter.deleteAllByMemberIdAndPhotoMemberId(requestMemberId, blockedMemberId)
        reviewableBodyPhotoAdapter.refresh(requestMemberId)

        reviewableBodyPhotoAdapter.deleteAllByMemberIdAndPhotoMemberId(blockedMemberId, requestMemberId)
        reviewableBodyPhotoAdapter.refresh(blockedMemberId)
    }

    @Transactional
    fun delete(memberId: Long, deletionReason: MemberDeletionReason, otherReason: String?) {
        val member = memberAdapter.getById(memberId)
        deletedMemberAdapter.create(member.memberId,
                                    member.memberUuid,
                                    member.nickname,
                                    member.mobileNumber,
                                    deletionReason,
                                    otherReason?.nullIfBlank())
        memberAdapter.deleteById(memberId)
        bodyPhotoAdapter.deleteAllByMemberId(memberId)
        reviewableBodyPhotoAdapter.deleteAllByMemberId(memberId)
        memberBlockRelationAdapter.deleteAllByMemberId(memberId)
    }

}