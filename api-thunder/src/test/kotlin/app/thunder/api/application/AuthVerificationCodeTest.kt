package app.thunder.api.application

import app.thunder.api.auth.JwtProperties
import app.thunder.api.auth.TokenManager
import app.thunder.api.config.SmsProperties
import app.thunder.api.controller.request.PostSmsRequest
import app.thunder.core.member.client.SmsClient
import app.thunder.core.member.repository.MemberRepository
import app.thunder.core.member.repository.MemberSettingRepository
import app.thunder.core.member.repository.MobileVerificationRepository
import java.security.SecureRandom
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.springframework.context.ApplicationEventPublisher

class AuthVerificationCodeTest {

    private val smsClient = mock(SmsClient::class.java)
    private val mobileVerificationRepository = mock(MobileVerificationRepository::class.java)

    @Test
    @DisplayName("인증번호는 SecureRandom이 뽑은 값으로 6자리를 만든다")
    fun verificationCodeComesFromSecureRandom() {
        // given
        val fixedRandom = object : SecureRandom() {
            override fun nextInt(bound: Int): Int = if (bound == VERIFICATION_CODE_RANGE) 554_321 else 0
        }
        val authService = AuthService(
            smsClient = smsClient,
            mobileVerificationRepository = mobileVerificationRepository,
            tokenManager = TokenManager(JwtProperties("test-jwt-secret-key-with-at-least-32-bytes", 1, 1)),
            applicationEventPublisher = mock(ApplicationEventPublisher::class.java),
            memberRepository = mock(MemberRepository::class.java),
            memberSettingRepository = mock(MemberSettingRepository::class.java),
            smsProperties = SmsProperties(),
            secureRandom = fixedRandom,
        )

        // when
        authService.sendSms(PostSmsRequest("device-1", "01012345678", "KR"))

        // then
        verify(mobileVerificationRepository).create("device-1", "01012345678", "KR", "654321")
        verify(smsClient).sendSms("01012345678", "인증번호 [654321]를 Thunder 앱에서 입력해주세요.", false)
    }

    companion object {
        private const val VERIFICATION_CODE_RANGE = 900_000
    }

}
