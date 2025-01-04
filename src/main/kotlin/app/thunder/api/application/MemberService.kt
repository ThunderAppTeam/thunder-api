package app.thunder.api.application

import app.thunder.api.adapter.sms.SmsAdapter
import app.thunder.api.controller.request.PostSignupRequest
import app.thunder.api.domain.member.MemberEntity
import app.thunder.api.domain.member.MemberRepository
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import kotlin.random.Random

@Service
class MemberService(
    private val smsAdapter: SmsAdapter,
    private val memberRepository: MemberRepository,
) {

    companion object {
        private const val VERIFICATION_MESSAGE = "[THUNDER]앱에서 인증번호를 입력해주세요: "
    }

    fun sendSms(mobileNumber: String) {
        val verificationCode = Random.nextInt(100000, 1000000).toString()
        smsAdapter.sendSms(mobileNumber, VERIFICATION_MESSAGE + verificationCode)
    }

    @Transactional
    fun signup(request: PostSignupRequest) {
        memberRepository.findByNickname(request.nickname)
            .ifPresent { throw RuntimeException("NICKNAME_DUPLICATED") }
        val newMember = MemberEntity.create(request.nickname,
                                            request.mobileNumber,
                                            request.mobileCountry,
                                            request.gender,
                                            request.birthDay,
                                            request.countryCode,
                                            request.marketingAgreement)
        memberRepository.save(newMember)
    }

}