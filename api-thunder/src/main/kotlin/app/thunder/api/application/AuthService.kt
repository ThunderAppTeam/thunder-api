package app.thunder.api.application

import app.thunder.api.config.SmsProperties
import app.thunder.api.application.dto.MemberAccessToken
import app.thunder.api.auth.TokenManager
import app.thunder.api.controller.request.PostSignupRequest
import app.thunder.api.controller.request.PostSmsRequest
import app.thunder.api.controller.request.PostSmsResetRequest
import app.thunder.api.event.RefreshReviewableEvent
import app.thunder.core.member.repository.MemberRepository
import app.thunder.core.member.domain.MemberSettingOptions
import app.thunder.core.member.repository.MemberSettingRepository
import app.thunder.core.member.repository.MobileVerificationRepository
import app.thunder.core.member.client.SmsClient
import app.thunder.core.member.command.CreateMemberCommand
import app.thunder.libraries.errors.CommonErrors.MISSING_REQUIRED_PARAMETER
import app.thunder.libraries.errors.MemberErrors.EXPIRED_MOBILE_VERIFICATION
import app.thunder.libraries.errors.MemberErrors.INVALID_MOBILE_VERIFICATION
import app.thunder.libraries.errors.MemberErrors.MOBILE_NOT_VERIFIED
import app.thunder.libraries.errors.MemberErrors.MOBILE_NUMBER_DUPLICATED
import app.thunder.libraries.errors.MemberErrors.NICKNAME_DUPLICATED
import app.thunder.libraries.errors.MemberErrors.NOT_FOUND_MOBILE_VERIFICATION
import app.thunder.libraries.errors.MemberErrors.TOO_MANY_MOBILE_VERIFICATION
import app.thunder.libraries.errors.MemberErrors.TOO_MANY_VERIFICATION_ATTEMPTS
import app.thunder.libraries.errors.ThunderException
import java.security.SecureRandom
import java.time.Duration
import java.time.LocalDateTime
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthService(
    private val smsClient: SmsClient,
    private val mobileVerificationRepository: MobileVerificationRepository,
    private val tokenManager: TokenManager,
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val memberRepository: MemberRepository,
    private val memberSettingRepository: MemberSettingRepository,
    private val smsProperties: SmsProperties,
    private val secureRandom: SecureRandom = SecureRandom(),
) {

    companion object {
        private const val MOBILE_NUMBER_FOR_TESTER = "01000000000"
        private const val VERIFICATION_CODE_FOR_TESTER = "250101"
        private const val MAX_UNVERIFIED_SENDS_PER_DAY = 5
        private const val VERIFICATION_CODE_MIN = 100000
        private const val VERIFICATION_CODE_RANGE = 900000
        private val SIGNUP_VERIFICATION_VALIDITY = Duration.ofMinutes(30)
    }

    @Transactional
    fun sendSms(request: PostSmsRequest) {
        mobileVerificationRepository.lockSendLimit(request.deviceId, request.mobileNumber)
        val yesterday = LocalDateTime.now().minusDays(1L)
        val deviceSendCount = mobileVerificationRepository.getCountByDeviceIdAndCreatedAtAfter(request.deviceId, yesterday)
        val mobileSendCount = mobileVerificationRepository.getCountByMobileNumberAndCreatedAtAfter(request.mobileNumber, yesterday)
        if (deviceSendCount >= MAX_UNVERIFIED_SENDS_PER_DAY || mobileSendCount >= MAX_UNVERIFIED_SENDS_PER_DAY) {
            throw ThunderException(TOO_MANY_MOBILE_VERIFICATION)
        }

        var verificationCode = (VERIFICATION_CODE_MIN + secureRandom.nextInt(VERIFICATION_CODE_RANGE)).toString()
        val isTestMode = smsProperties.testModeEnabled
        if (isTestMode && request.mobileNumber == MOBILE_NUMBER_FOR_TESTER) {
            verificationCode = VERIFICATION_CODE_FOR_TESTER
        }

        mobileVerificationRepository.create(request.deviceId,
                                      request.mobileNumber,
                                      request.mobileCountry,
                                      verificationCode)

        smsClient.sendSms(request.mobileNumber, "인증번호 [${verificationCode}]를 Thunder 앱에서 입력해주세요.", isTestMode)
    }

    @Transactional(noRollbackFor = [ThunderException::class])
    fun verifySms(deviceId: String, mobileNumber: String, verificationCode: String): MemberAccessToken {
        val verification = mobileVerificationRepository.getLastByDeviceIdAndMobileNumber(deviceId, mobileNumber)
            ?: throw ThunderException(NOT_FOUND_MOBILE_VERIFICATION)

        if (verification.isExpired()) {
            throw ThunderException(EXPIRED_MOBILE_VERIFICATION)
        }
        if (verification.isLocked()) {
            throw ThunderException(TOO_MANY_VERIFICATION_ATTEMPTS)
        }
        if (verification.verificationCode != verificationCode) {
            mobileVerificationRepository.increaseFailedAttemptCount(verification.mobileVerificationId)
            throw ThunderException(INVALID_MOBILE_VERIFICATION)
        }
        if (!mobileVerificationRepository.verify(verification.mobileVerificationId)) {
            throw ThunderException(INVALID_MOBILE_VERIFICATION)
        }

        val member = memberRepository.getByMobileNumber(mobileNumber)
            ?: return MemberAccessToken(member = null, accessToken = null)
        memberRepository.revokeTokensIssuedBefore(member.memberId, LocalDateTime.now())
        return MemberAccessToken(member = member,
                                 accessToken = tokenManager.generateAccessToken(member.memberId))
    }

    @Transactional
    fun resetSendLimit(request: PostSmsResetRequest) {
        if (request.deviceId == null && request.mobileNumber == null) {
            throw ThunderException(MISSING_REQUIRED_PARAMETER)
        }
        request.deviceId?.let { mobileVerificationRepository.resetByDeviceId(it) }
        request.mobileNumber?.let { mobileVerificationRepository.resetByMobileNumber(it) }
    }

    @Transactional
    fun signup(request: PostSignupRequest): MemberAccessToken {
        requireVerifiedMobileNumber(request.mobileNumber)
        this.isAvailableNickName(request.nickname)
        val duplicatedMobileNumber = memberRepository.getByMobileNumber(request.mobileNumber) != null
        if (duplicatedMobileNumber) {
            throw ThunderException(MOBILE_NUMBER_DUPLICATED)
        }

        val command = CreateMemberCommand(
            nickname = request.nickname,
            mobileCountry = request.mobileCountry,
            mobileNumber = request.mobileNumber,
            gender = request.gender,
            birthDay = request.birthDay,
            countryCode = request.countryCode,
            marketingAgreement = request.marketingAgreement
        )
        val member = memberRepository.create(command)
        val memberSettingOptions = MemberSettingOptions(reviewCompleteNotify = true,
                                                        reviewRequestNotify = true,
                                                        marketingAgreement = request.marketingAgreement)
        memberSettingRepository.create(member.memberId, memberSettingOptions)
        applicationEventPublisher.publishEvent(RefreshReviewableEvent(member.memberId))

        return MemberAccessToken(
            member = member,
            accessToken = tokenManager.generateAccessToken(member.memberId),
        )
    }

    private fun requireVerifiedMobileNumber(mobileNumber: String) {
        val verifiedAfter = LocalDateTime.now().minus(SIGNUP_VERIFICATION_VALIDITY)
        if (!mobileVerificationRepository.existsVerifiedByMobileNumberAfter(mobileNumber, verifiedAfter)) {
            throw ThunderException(MOBILE_NOT_VERIFIED)
        }
    }

    @Transactional(readOnly = true)
    fun isAvailableNickName(nickname: String) {
        val isPresent = memberRepository.getByNickname(nickname)
        if (isPresent != null) {
            throw ThunderException(NICKNAME_DUPLICATED)
        }
    }

}
