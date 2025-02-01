package app.thunder.api.application

import app.thunder.api.adapter.sms.SmsAdapter
import app.thunder.api.application.SupplyReviewableEventHandler.Companion.REVIEWABLE_QUEUE_MINIMUM_SIZE
import app.thunder.api.auth.TokenManager
import app.thunder.api.controller.request.PostSignupRequest
import app.thunder.api.controller.request.PostSmsRequest
import app.thunder.api.controller.request.PostSmsResetRequest
import app.thunder.api.domain.body.ReviewableBodyPhotoAdapter
import app.thunder.api.domain.member.Member
import app.thunder.api.domain.member.MemberAdapter
import app.thunder.api.domain.member.MemberBlockRelationAdapter
import app.thunder.api.domain.member.MemberEntity
import app.thunder.api.domain.member.MemberRepository
import app.thunder.api.domain.member.MobileVerificationEntity
import app.thunder.api.domain.member.MobileVerificationRepository
import app.thunder.api.domain.member.findAllByDeviceIdAndCreatedAtAfter
import app.thunder.api.domain.member.findAllByDeviceIdAndNotVerify
import app.thunder.api.domain.member.findAllByMobileNumber
import app.thunder.api.domain.member.findLastByDeviceIdAndMobileNumber
import app.thunder.api.exception.CommonErrors.MISSING_REQUIRED_PARAMETER
import app.thunder.api.exception.MemberErrors.EXPIRED_MOBILE_VERIFICATION
import app.thunder.api.exception.MemberErrors.INVALID_MOBILE_VERIFICATION
import app.thunder.api.exception.MemberErrors.NICKNAME_DUPLICATED
import app.thunder.api.exception.MemberErrors.NOT_FOUND_MOBILE_VERIFICATION
import app.thunder.api.exception.MemberErrors.TOO_MANY_MOBILE_VERIFICATION
import app.thunder.api.exception.ThunderException
import java.time.LocalDateTime
import kotlin.random.Random
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate

@Service
class MemberService(
    private val smsAdapter: SmsAdapter,
    private val memberRepository: MemberRepository,
    private val mobileVerificationRepository: MobileVerificationRepository,
    private val tokenManager: TokenManager,
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val memberBlockRelationAdapter: MemberBlockRelationAdapter,
    private val memberAdapter: MemberAdapter,
    private val reviewableBodyPhotoAdapter: ReviewableBodyPhotoAdapter,
    private val transactionTemplate: TransactionTemplate,
) {

    @Transactional
    fun sendSms(request: PostSmsRequest): String {
        val yesterday = LocalDateTime.now().minusDays(1L)
        val histories = mobileVerificationRepository.findAllByDeviceIdAndCreatedAtAfter(request.deviceId, yesterday)
        if (!request.isTestMode && histories.size >= 5) {
            throw ThunderException(TOO_MANY_MOBILE_VERIFICATION)
        }

        val verificationCode = Random.nextInt(100000, 1000000).toString()
        mobileVerificationRepository.save(
            MobileVerificationEntity.create(request.deviceId,
                                            request.mobileNumber,
                                            request.mobileCountry,
                                            verificationCode)
        )

        smsAdapter.sendSms(request.mobileNumber, "인증번호 [${verificationCode}]를 Thunder 앱에서 입력해주세요.", request.isTestMode)
        return verificationCode
    }

    @Transactional
    fun verifySms(deviceId: String, mobileNumber: String, verificationCode: String): String? {
        val verification = mobileVerificationRepository.findLastByDeviceIdAndMobileNumber(deviceId, mobileNumber)
            ?: throw ThunderException(NOT_FOUND_MOBILE_VERIFICATION)

        if (verification.isExpired()) {
            throw ThunderException(EXPIRED_MOBILE_VERIFICATION)
        }
        if (verification.verificationCode != verificationCode) {
            throw ThunderException(INVALID_MOBILE_VERIFICATION)
        }
        verification.verified()

        return memberRepository.findByMobileNumber(mobileNumber)
            .map { tokenManager.generateAccessToken(it.memberId) }
            .orElse(null)
    }

    @Transactional
    fun resetSendLimit(request: PostSmsResetRequest) {
        when {
            request.deviceId != null -> mobileVerificationRepository.findAllByDeviceIdAndNotVerify(request.deviceId)
            request.mobileNumber != null -> mobileVerificationRepository.findAllByMobileNumber(request.mobileNumber)
            else -> throw ThunderException(MISSING_REQUIRED_PARAMETER)
        }.forEach { it.reset() }
    }

    @Transactional
    fun signup(request: PostSignupRequest): Member {
        memberRepository.findByNickname(request.nickname)
            .ifPresent { throw ThunderException(NICKNAME_DUPLICATED) }
        val newMember = MemberEntity.create(request.nickname,
                                            request.mobileNumber,
                                            request.mobileCountry,
                                            request.gender,
                                            request.birthDay,
                                            request.countryCode,
                                            request.marketingAgreement)
        memberRepository.save(newMember)
        applicationEventPublisher.publishEvent(SupplyReviewableEvent(newMember.memberId))
        return Member.from(newMember)
    }

    @Transactional(readOnly = true)
    fun isAvailableNickName(nickname: String) {
        val isPresent = memberRepository.findByNickname(nickname).isPresent
        if (isPresent) {
            throw ThunderException(NICKNAME_DUPLICATED)
        }
    }

    fun block(requestMemberId: Long, blockedMemberId: Long) {
        transactionTemplate.execute { status ->
            memberAdapter.getById(blockedMemberId)
            memberBlockRelationAdapter.create(memberId = blockedMemberId,
                                              blockedMemberId = requestMemberId,
                                              createdBy = requestMemberId)
            memberBlockRelationAdapter.create(memberId = requestMemberId,
                                              blockedMemberId = blockedMemberId,
                                              createdBy = requestMemberId)
            status.flush()
            true
        }

        val deleteAllBlockedMemberBodyPhotoInQueue = { blocker: Long, blocked: Long ->
            reviewableBodyPhotoAdapter.getAllByMemberId(blocker).asSequence()
                .filter { it.bodyPhotoMemberId == blocked }
                .forEach { reviewableBodyPhotoAdapter.deleteByMemberIdAndBodyPhotoId(blocker, it.bodyPhotoId) }
            val blockedMemberQueueSize = reviewableBodyPhotoAdapter.getAllByMemberId(blocker).size
            if (blockedMemberQueueSize <= REVIEWABLE_QUEUE_MINIMUM_SIZE) {
                applicationEventPublisher.publishEvent(SupplyReviewableEvent(blocker))
            }
        }

        transactionTemplate.execute { status ->
            deleteAllBlockedMemberBodyPhotoInQueue(blockedMemberId, requestMemberId)
            deleteAllBlockedMemberBodyPhotoInQueue(requestMemberId, blockedMemberId)
            status.flush()
            true
        }
    }

}