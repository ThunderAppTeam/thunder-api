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
import app.thunder.libraries.errors.MemberErrors.MOBILE_NUMBER_DUPLICATED
import app.thunder.libraries.errors.MemberErrors.NICKNAME_DUPLICATED
import app.thunder.libraries.errors.MemberErrors.NOT_FOUND_MOBILE_VERIFICATION
import app.thunder.libraries.errors.MemberErrors.TOO_MANY_MOBILE_VERIFICATION
import app.thunder.libraries.errors.ThunderException
import java.time.LocalDateTime
import kotlin.random.Random
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
) {

    companion object {
        private const val MOBILE_NUMBER_FOR_TESTER = "01000000000"
        private const val VERIFICATION_CODE_FOR_TESTER = "250101"
    }

    @Transactional
    fun sendSms(request: PostSmsRequest) {
        val yesterday = LocalDateTime.now().minusDays(1L)
        val sendCount = mobileVerificationRepository.getCountByDeviceIdAndCreatedAtAfter(request.deviceId, yesterday)
        if (sendCount >= 5) {
            throw ThunderException(TOO_MANY_MOBILE_VERIFICATION)
        }

        var verificationCode = Random.nextInt(100000, 1000000).toString()
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

    @Transactional
    fun verifySms(deviceId: String, mobileNumber: String, verificationCode: String): MemberAccessToken {
        val verification = mobileVerificationRepository.getLastByDeviceIdAndMobileNumber(deviceId, mobileNumber)
            ?: throw ThunderException(NOT_FOUND_MOBILE_VERIFICATION)

        if (verification.isExpired()) {
            throw ThunderException(EXPIRED_MOBILE_VERIFICATION)
        }
        if (verification.verificationCode != verificationCode) {
            throw ThunderException(INVALID_MOBILE_VERIFICATION)
        }
        mobileVerificationRepository.verify(verification.mobileVerificationId)

        val member = memberRepository.getByMobileNumber(mobileNumber)
        val accessToken = member?.let { tokenManager.generateAccessToken(member.memberId) }
        return MemberAccessToken(member = member,
                                 accessToken = accessToken)
    }

    @Transactional
    fun resetSendLimit(request: PostSmsResetRequest) {
        when {
            request.deviceId != null -> mobileVerificationRepository.resetByDeviceId(request.deviceId)
            request.mobileNumber != null -> mobileVerificationRepository.resetByMobileNumber(request.mobileNumber)
            else -> throw ThunderException(MISSING_REQUIRED_PARAMETER)
        }
    }

    @Transactional
    fun signup(request: PostSignupRequest): MemberAccessToken {
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

    @Transactional(readOnly = true)
    fun isAvailableNickName(nickname: String) {
        val isPresent = memberRepository.getByNickname(nickname)
        if (isPresent != null) {
            throw ThunderException(NICKNAME_DUPLICATED)
        }
    }

}
