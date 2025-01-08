package app.thunder.api.application

import app.thunder.api.adapter.sms.SmsAdapter
import app.thunder.api.controller.request.PostSignupRequest
import app.thunder.api.controller.request.PostSmsRequest
import app.thunder.api.domain.member.*
import app.thunder.api.exception.MemberErrors.*
import app.thunder.api.exception.ThunderException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import kotlin.random.Random

@Service
class MemberService(
    private val smsAdapter: SmsAdapter,
    private val memberRepository: MemberRepository,
    private val mobileVerificationRepository: MobileVerificationRepository
) {

    companion object {
        private const val VERIFICATION_MESSAGE = "[THUNDER]앱에서 인증번호를 입력해주세요: "
    }


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

        smsAdapter.sendSms(request.mobileNumber, VERIFICATION_MESSAGE + verificationCode, request.isTestMode)
        return verificationCode
    }

    @Transactional
    fun verifySms(deviceId: String, mobileNumber: String, verificationCode: String) {
        val verification = mobileVerificationRepository.findLastByDeviceIdAndMobileNumber(deviceId, mobileNumber)
            ?: throw ThunderException(NOT_FOUND_MOBILE_VERIFICATION)

        if (verification.isExpired()) {
            throw ThunderException(EXPIRED_MOBILE_VERIFICATION)
        }
        if (verification.verificationCode != verificationCode) {
            throw ThunderException(INVALID_MOBILE_VERIFICATION)
        }
        verification.verified()
    }

    @Transactional
    fun signup(request: PostSignupRequest) {
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
    }

    @Transactional(readOnly = true)
    fun isAvailableNickName(nickname: String): Boolean {
        return memberRepository.findByNickname(nickname).isEmpty
    }

}