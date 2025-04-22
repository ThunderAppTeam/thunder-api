package app.thunder.api.application

import app.thunder.api.controller.request.PutMemberSettingsRequest
import app.thunder.api.event.RefreshReviewableEvent
import app.thunder.libraries.errors.MemberErrors.CANNOT_BLOCK_SELF
import app.thunder.libraries.errors.MemberErrors.NOT_FOUND_MEMBER
import app.thunder.libraries.errors.ThunderException
import app.thunder.api.func.nullIfBlank
import app.thunder.core.member.repository.DeletedMemberRepository
import app.thunder.core.member.repository.FcmTokenRepository
import app.thunder.core.member.domain.Member
import app.thunder.core.member.repository.MemberBlockRelationRepository
import app.thunder.core.member.domain.MemberDeletionReason
import app.thunder.core.member.repository.MemberRepository
import app.thunder.core.member.domain.MemberSetting
import app.thunder.core.member.domain.MemberSettingOptions
import app.thunder.core.member.repository.MemberSettingRepository
import app.thunder.core.flag.repository.FlagHistoryRepository
import app.thunder.core.photo.repository.BodyPhotoRepository
import app.thunder.core.review.repository.BodyReviewRepository
import app.thunder.core.review.repository.DummyDeckRepository
import app.thunder.core.review.repository.ReviewableBodyPhotoRepository
import java.time.LocalDateTime
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MemberService(
    private val memberRepository: MemberRepository,
    private val memberBlockRelationRepository: MemberBlockRelationRepository,
    private val deletedMemberRepository: DeletedMemberRepository,
    private val bodyPhotoRepository: BodyPhotoRepository,
    private val reviewableBodyPhotoRepository: ReviewableBodyPhotoRepository,
    private val fcmTokenRepository: FcmTokenRepository,
    private val memberSettingRepository: MemberSettingRepository,
    private val bodyReviewRepository: BodyReviewRepository,
    private val flagHistoryRepository: FlagHistoryRepository,
    private val dummyDeckRepository: DummyDeckRepository,
    private val bodyPhotoStorageCleaner: BodyPhotoStorageCleaner,
    private val applicationEventPublisher: ApplicationEventPublisher,
) {

    @Transactional(readOnly = true)
    fun getById(memberId: Long): Member {
        return memberRepository.getById(memberId)
            ?: throw ThunderException(NOT_FOUND_MEMBER)
    }

    @Transactional
    fun block(requestMemberId: Long, blockedMemberId: Long) {
        if (requestMemberId == blockedMemberId) {
            throw ThunderException(CANNOT_BLOCK_SELF)
        }

        listOf(requestMemberId, blockedMemberId).sorted().forEach(reviewableBodyPhotoRepository::lockQueue)
        memberRepository.getById(blockedMemberId)
            ?: throw ThunderException(NOT_FOUND_MEMBER)

        memberBlockRelationRepository.create(memberId = blockedMemberId,
                                       blockedMemberId = requestMemberId,
                                       createdBy = requestMemberId)
        memberBlockRelationRepository.create(memberId = requestMemberId,
                                       blockedMemberId = blockedMemberId,
                                       createdBy = requestMemberId)

        reviewableBodyPhotoRepository.deleteAllByMemberIdAndPhotoMemberId(requestMemberId, blockedMemberId)
        applicationEventPublisher.publishEvent(RefreshReviewableEvent(requestMemberId))

        reviewableBodyPhotoRepository.deleteAllByMemberIdAndPhotoMemberId(blockedMemberId, requestMemberId)
        applicationEventPublisher.publishEvent(RefreshReviewableEvent(blockedMemberId))
    }

    @Transactional
    fun delete(memberId: Long, deletionReason: MemberDeletionReason, otherReason: String?) {
        reviewableBodyPhotoRepository.lockQueue(memberId)
        val member = this.getById(memberId)
        deletedMemberRepository.create(member.memberId,
                                 member.memberUuid,
                                 member.nickname,
                                 member.mobileNumber,
                                 deletionReason,
                                 otherReason?.nullIfBlank())
        memberRepository.deleteById(memberId)
        deleteBodyPhotos(memberId)
        reviewableBodyPhotoRepository.deleteAllByMemberId(memberId)
        memberBlockRelationRepository.deleteAllByMemberId(memberId)
        memberSettingRepository.deleteByMemberId(memberId)
        fcmTokenRepository.deleteByMemberId(memberId)
        dummyDeckRepository.deleteAllByMemberId(memberId)
    }

    private fun deleteBodyPhotos(memberId: Long) {
        val bodyPhotos = bodyPhotoRepository.getAllByMemberId(memberId)
        bodyPhotos.forEach { bodyPhoto ->
            bodyReviewRepository.deleteAllByBodyPhotoId(bodyPhoto.bodyPhotoId)
            flagHistoryRepository.deleteAllByBodyPhotoId(bodyPhoto.bodyPhotoId)
        }
        bodyPhotoRepository.deleteAllByMemberId(memberId)
        bodyPhotoStorageCleaner.deleteAfterCommit(bodyPhotos.map { it.imageUrl })
    }

    @Transactional
    fun logout(memberId: Long) {
        memberRepository.revokeTokensIssuedBefore(memberId, LocalDateTime.now())
        fcmTokenRepository.deleteByMemberId(memberId)
    }

    @Transactional
    fun savedFcmToken(memberId: Long, fcmToken: String) {
        fcmTokenRepository.createOrUpdate(memberId, fcmToken)
    }

    @Transactional
    fun getSettings(memberId: Long): MemberSetting {
        return memberSettingRepository.getByMemberId(memberId)
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
        memberSettingRepository.update(settings)
    }

}
