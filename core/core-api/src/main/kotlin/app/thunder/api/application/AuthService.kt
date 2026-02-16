package app.thunder.api.application

import app.thunder.api.adapter.sms.SmsAdapter
import app.thunder.api.application.dto.MemberAccessToken
import app.thunder.api.auth.TokenManager
import app.thunder.api.controller.request.PostSignupRequest
import app.thunder.api.controller.request.PostSmsRequest
import app.thunder.api.controller.request.PostSmsResetRequest
import app.thunder.api.event.RefreshReviewableEvent
import app.thunder.api.exception.CommonErrors.MISSING_REQUIRED_PARAMETER
import app.thunder.api.exception.MemberErrors.EXPIRED_MOBILE_VERIFICATION
import app.thunder.api.exception.MemberErrors.INVALID_MOBILE_VERIFICATION
import app.thunder.api.exception.MemberErrors.MOBILE_NUMBER_DUPLICATED
import app.thunder.api.exception.MemberErrors.NICKNAME_DUPLICATED
import app.thunder.api.exception.MemberErrors.NOT_FOUND_MOBILE_VERIFICATION
import app.thunder.api.exception.MemberErrors.TOO_MANY_MOBILE_VERIFICATION
import app.thunder.api.exception.ThunderException
import app.thunder.domain.member.MemberPort
import app.thunder.domain.member.MemberSettingPort
import app.thunder.domain.member.MemberSettingOptions
import app.thunder.domain.member.MobileVerificationPort
import app.thunder.domain.member.command.CreateMemberCommand
import java.time.LocalDateTime
import kotlin.random.Random
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthService(
    private val smsAdapter: SmsAdapter,
    private val mobileVerificationPort: MobileVerificationPort,
    private val tokenManager: TokenManager,
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val memberAdapter: MemberPort,
    private val memberSettingAdapter: MemberSettingPort,
) {

    companion object {
        private const val MOBILE_NUMBER_FOR_TESTER = "01000000000"
        private const val VERIFICATION_CODE_FOR_TESTER = "250101"
    }

    @Transactional
    fun sendSms(request: PostSmsRequest): String {
        val yesterday = LocalDateTime.now().minusDays(1L)
        val sendCount = mobileVerificationPort.getCountByDeviceIdAndCreatedAtAfter(request.deviceId, yesterday)
        if (!request.isTestMode && sendCount >= 5) {
            throw ThunderException(TOO_MANY_MOBILE_VERIFICATION)
        }

        var verificationCode = Random.nextInt(100000, 1000000).toString()
        var isTestMode = request.isTestMode
        if (request.mobileNumber == MOBILE_NUMBER_FOR_TESTER) {
            verificationCode = VERIFICATION_CODE_FOR_TESTER
            isTestMode = true
        }

        mobileVerificationPort.create(request.deviceId,
                                      request.mobileNumber,
                                      request.mobileCountry,
                                      verificationCode)

        smsAdapter.sendSms(request.mobileNumber, "인증번호 [${verificationCode}]를 Thunder 앱에서 입력해주세요.", isTestMode)
        return verificationCode
    }

    @Transactional
    fun verifySms(deviceId: String, mobileNumber: String, verificationCode: String): MemberAccessToken {
        val verification = mobileVerificationPort.getLastByDeviceIdAndMobileNumber(deviceId, mobileNumber)
            ?: throw ThunderException(NOT_FOUND_MOBILE_VERIFICATION)

        if (verification.isExpired()) {
            throw ThunderException(EXPIRED_MOBILE_VERIFICATION)
        }
        if (verification.verificationCode != verificationCode) {
            throw ThunderException(INVALID_MOBILE_VERIFICATION)
        }
        mobileVerificationPort.verify(verification.mobileVerificationId)

        val member = memberAdapter.getByMobileNumber(mobileNumber)
        val accessToken = member?.let { tokenManager.generateAccessToken(member.memberId) }
        return MemberAccessToken(member = member,
                                 accessToken = accessToken)
    }

    @Transactional
    fun resetSendLimit(request: PostSmsResetRequest) {
        when {
            request.deviceId != null -> mobileVerificationPort.resetByDeviceId(request.deviceId)
            request.mobileNumber != null -> mobileVerificationPort.resetByMobileNumber(request.mobileNumber)
            else -> throw ThunderException(MISSING_REQUIRED_PARAMETER)
        }
    }

    @Transactional
    fun signup(request: PostSignupRequest): MemberAccessToken {
        this.isAvailableNickName(request.nickname)
        val duplicatedMobileNumber = memberAdapter.getByMobileNumber(request.mobileNumber) != null
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
        val member = memberAdapter.create(command)
        val memberSettingOptions = MemberSettingOptions(reviewCompleteNotify = true,
                                                        reviewRequestNotify = true,
                                                        marketingAgreement = request.marketingAgreement)
        memberSettingAdapter.create(member.memberId, memberSettingOptions)
        applicationEventPublisher.publishEvent(RefreshReviewableEvent(member.memberId))

        return MemberAccessToken(
            member = member,
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
