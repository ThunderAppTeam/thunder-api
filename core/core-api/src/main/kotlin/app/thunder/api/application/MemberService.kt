package app.thunder.api.application

import app.thunder.api.controller.request.PutMemberSettingsRequest
import app.thunder.api.event.RefreshReviewableEvent
import app.thunder.api.exception.MemberErrors.NOT_FOUND_MEMBER
import app.thunder.api.exception.ThunderException
import app.thunder.api.func.nullIfBlank
import app.thunder.domain.member.DeletedMemberPort
import app.thunder.domain.member.FcmTokenPort
import app.thunder.domain.member.Member
import app.thunder.domain.member.MemberDeletionReason
import app.thunder.domain.member.MemberBlockRelationPort
import app.thunder.domain.member.MemberPort
import app.thunder.domain.member.MemberSetting
import app.thunder.domain.member.MemberSettingOptions
import app.thunder.domain.member.MemberSettingPort
import app.thunder.domain.photo.BodyPhotoPort
import app.thunder.domain.review.ReviewableBodyPhotoPort
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MemberService(
    private val memberAdapter: MemberPort,
    private val memberBlockRelationPort: MemberBlockRelationPort,
    private val deletedMemberPort: DeletedMemberPort,
    private val bodyPhotoAdapter: BodyPhotoPort,
    private val reviewableBodyPhotoAdapter: ReviewableBodyPhotoPort,
    private val fcmTokenPort: FcmTokenPort,
    private val memberSettingAdapter: MemberSettingPort,
    private val applicationEventPublisher: ApplicationEventPublisher,
) {

    @Transactional(readOnly = true)
    fun getById(memberId: Long): Member {
        return memberAdapter.getById(memberId)
            ?: throw ThunderException(NOT_FOUND_MEMBER)
    }

    @Transactional
    fun block(requestMemberId: Long, blockedMemberId: Long) {
        memberBlockRelationPort.create(memberId = blockedMemberId,
                                       blockedMemberId = requestMemberId,
                                       createdBy = requestMemberId)
        memberBlockRelationPort.create(memberId = requestMemberId,
                                       blockedMemberId = blockedMemberId,
                                       createdBy = requestMemberId)

        reviewableBodyPhotoAdapter.deleteAllByMemberIdAndPhotoMemberId(requestMemberId, blockedMemberId)
        applicationEventPublisher.publishEvent(RefreshReviewableEvent(requestMemberId))

        reviewableBodyPhotoAdapter.deleteAllByMemberIdAndPhotoMemberId(blockedMemberId, requestMemberId)
        applicationEventPublisher.publishEvent(RefreshReviewableEvent(blockedMemberId))
    }

    @Transactional
    fun delete(memberId: Long, deletionReason: MemberDeletionReason, otherReason: String?) {
        val member = this.getById(memberId)
        deletedMemberPort.create(member.memberId,
                                 member.memberUuid,
                                 member.nickname,
                                 member.mobileNumber,
                                 deletionReason,
                                 otherReason?.nullIfBlank())
        memberAdapter.deleteById(memberId)
        bodyPhotoAdapter.deleteAllByMemberId(memberId)
        reviewableBodyPhotoAdapter.deleteAllByMemberId(memberId)
        memberBlockRelationPort.deleteAllByMemberId(memberId)
    }

    @Transactional
    fun logout(memberId: Long) {
        val member = this.getById(memberId)
        member.logout(memberId)
        memberAdapter.update(member)
    }

    @Transactional
    fun savedFcmToken(memberId: Long, fcmToken: String) {
        fcmTokenPort.createOrUpdate(memberId, fcmToken)
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
