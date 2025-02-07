package app.thunder.api.application

import app.thunder.api.adapter.sms.SmsAdapter
import app.thunder.api.application.dto.MemberAccessToken
import app.thunder.api.auth.TokenManager
import app.thunder.api.controller.request.PostSignupRequest
import app.thunder.api.controller.request.PostSmsRequest
import app.thunder.api.controller.request.PostSmsResetRequest
import app.thunder.api.domain.member.adapter.MemberAdapter
import app.thunder.api.domain.member.entity.MobileVerificationEntity
import app.thunder.api.domain.member.repository.MobileVerificationRepository
import app.thunder.api.domain.member.repository.findAllByDeviceIdAndCreatedAtAfter
import app.thunder.api.domain.member.repository.findAllByDeviceIdAndNotVerify
import app.thunder.api.domain.member.repository.findAllByMobileNumber
import app.thunder.api.domain.member.repository.findLastByDeviceIdAndMobileNumber
import app.thunder.api.exception.CommonErrors.MISSING_REQUIRED_PARAMETER
import app.thunder.api.exception.MemberErrors.EXPIRED_MOBILE_VERIFICATION
import app.thunder.api.exception.MemberErrors.INVALID_MOBILE_VERIFICATION
import app.thunder.api.exception.MemberErrors.MOBILE_NUMBER_DUPLICATED
import app.thunder.api.exception.MemberErrors.NICKNAME_DUPLICATED
import app.thunder.api.exception.MemberErrors.NOT_FOUND_MOBILE_VERIFICATION
import app.thunder.api.exception.MemberErrors.TOO_MANY_MOBILE_VERIFICATION
import app.thunder.api.exception.ThunderException
import java.time.LocalDateTime
import kotlin.random.Random
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthService(
    private val smsAdapter: SmsAdapter,
    private val mobileVerificationRepository: MobileVerificationRepository, // TODO: need to change adapter
    private val tokenManager: TokenManager,
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val memberAdapter: MemberAdapter,
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
    fun verifySms(deviceId: String, mobileNumber: String, verificationCode: String): MemberAccessToken {
        val verification = mobileVerificationRepository.findLastByDeviceIdAndMobileNumber(deviceId, mobileNumber)
            ?: throw ThunderException(NOT_FOUND_MOBILE_VERIFICATION)

        if (verification.isExpired()) {
            throw ThunderException(EXPIRED_MOBILE_VERIFICATION)
        }
        if (verification.verificationCode != verificationCode) {
            throw ThunderException(INVALID_MOBILE_VERIFICATION)
        }
        verification.verified()

        val member = memberAdapter.getByMobileNumber(mobileNumber)
        val accessToken = member?.let { tokenManager.generateAccessToken(member.memberId) }
        return MemberAccessToken(memberId = member?.memberId,
                                 accessToken = accessToken)
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
    fun signup(request: PostSignupRequest): MemberAccessToken {
        this.isAvailableNickName(request.nickname)
        val duplicatedMobileNumber = memberAdapter.getByMobileNumber(request.mobileNumber) != null
        if (duplicatedMobileNumber) {
            throw ThunderException(MOBILE_NUMBER_DUPLICATED)
        }

        val member = memberAdapter.create(request)
        applicationEventPublisher.publishEvent(SupplyReviewableEvent(member.memberId))

        return MemberAccessToken(
            memberId = member.memberId,
            accessToken = tokenManager.generateAccessToken(member.memberId),
        )
    }

    @Transactional(readOnly = true)
    fun isAvailableNickName(nickname: String) {
        val isPresent = memberAdapter.getByNickname(nickname)
        if (isPresent != null) {
            throw ThunderException(NICKNAME_DUPLICATED)
        }
    }

}