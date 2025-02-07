package app.thunder.api.application

import app.thunder.api.domain.member.Member
import app.thunder.api.domain.member.adapter.MemberAdapter
import app.thunder.api.domain.member.adapter.MemberBlockRelationAdapter
import app.thunder.api.domain.review.adapter.ReviewableBodyPhotoAdapter
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MemberService(
    private val memberAdapter: MemberAdapter,
    private val memberBlockRelationAdapter: MemberBlockRelationAdapter,
    private val reviewableBodyPhotoAdapter: ReviewableBodyPhotoAdapter,
) {

    @Transactional(readOnly = true)
    fun getById(memberId: Long): Member {
        return memberAdapter.getById(memberId)
    }

    @Transactional(readOnly = true)
    fun block(requestMemberId: Long, blockedMemberId: Long) {
        memberBlockRelationAdapter.create(memberId = blockedMemberId,
                                          blockedMemberId = requestMemberId,
                                          createdBy = requestMemberId)
        memberBlockRelationAdapter.create(memberId = requestMemberId,
                                          blockedMemberId = blockedMemberId,
                                          createdBy = requestMemberId)

        reviewableBodyPhotoAdapter.deleteAllByMemberIdAndPhotoMemberId(requestMemberId, blockedMemberId)
        reviewableBodyPhotoAdapter.refresh(blockedMemberId)
        reviewableBodyPhotoAdapter.refresh(requestMemberId)
    }

}