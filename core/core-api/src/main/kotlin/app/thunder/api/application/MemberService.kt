package app.thunder.api.application

import app.thunder.api.controller.request.PutMemberSettingsRequest
import app.thunder.api.domain.member.adapter.DeletedMemberAdapter
import app.thunder.api.domain.member.adapter.FcmTokenAdapter
import app.thunder.api.domain.member.adapter.MemberAdapter
import app.thunder.api.domain.member.adapter.MemberBlockRelationAdapter
import app.thunder.api.domain.member.adapter.MemberSettingAdapter
import app.thunder.api.domain.review.adapter.ReviewableBodyPhotoAdapter
import app.thunder.api.exception.MemberErrors.NOT_FOUND_MEMBER
import app.thunder.api.exception.ThunderException
import app.thunder.api.func.nullIfBlank
import app.thunder.domain.member.Member
import app.thunder.domain.member.MemberDeletionReason
import app.thunder.domain.member.MemberSetting
import app.thunder.domain.member.MemberSettingOptions
import app.thunder.domain.photo.BodyPhotoAdapter
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MemberService(
    private val memberAdapter: MemberAdapter,
    private val memberBlockRelationAdapter: MemberBlockRelationAdapter,
    private val deletedMemberAdapter: DeletedMemberAdapter,
    private val bodyPhotoAdapter: BodyPhotoAdapter,
    private val reviewableBodyPhotoAdapter: ReviewableBodyPhotoAdapter,
    private val fcmTokenAdapter: FcmTokenAdapter,
    private val memberSettingAdapter: MemberSettingAdapter,
) {

    @Transactional(readOnly = true)
    fun getById(memberId: Long): Member {
        return memberAdapter.getById(memberId)
            ?: throw ThunderException(NOT_FOUND_MEMBER)
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
        val member = this.getById(memberId)
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

    @Transactional
    fun logout(memberId: Long) {
        val member = this.getById(memberId)
        member.logout(memberId)
        memberAdapter.update(member)
    }

    @Transactional
    fun savedFcmToken(memberId: Long, fcmToken: String) {
        fcmTokenAdapter.createOrUpdate(memberId, fcmToken)
    }

    @Transactional
    fun getSettings(memberId: Long): MemberSetting {
        return memberSettingAdapter.getByMemberId(memberId)
            ?: throw ThunderException(NOT_FOUND_MEMBER)
    }

    @Transactional
    fun updateSettings(memberId: Long, request: PutMemberSettingsRequest) {
        val options = MemberSettingOptions(
            reviewCompleteNotify = request.reviewCompleteNotify,
            reviewRequestNotify = request.reviewRequestNotify,
            marketingAgreement = request.marketingAgreement
        )

        val settings = this.getSettings(memberId)
        settings.setOptions(options)
        memberSettingAdapter.update(settings)
    }

}