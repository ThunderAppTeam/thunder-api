package app.thunder.api.application

import app.thunder.api.adapter.sms.SmsAdapter
import org.springframework.stereotype.Service
import kotlin.random.Random

@Service
class MemberService(
    private val smsAdapter: SmsAdapter,
) {

    companion object {
        private const val VERIFICATION_MESSAGE = "[THUNDER]앱에서 인증번호를 입력해주세요: "
    }

    fun sendSms(mobileNumber: String) {
        val verificationCode = Random.nextInt(100000, 1000000).toString()
        smsAdapter.sendSms(mobileNumber, VERIFICATION_MESSAGE + verificationCode)
    }

}