package app.thunder.api.application

import app.thunder.api.auth.TokenManager
import app.thunder.api.config.SmsProperties
import app.thunder.core.member.client.SmsClient
import app.thunder.core.member.repository.MemberRepository
import app.thunder.core.member.repository.MemberSettingRepository
import app.thunder.core.member.repository.MobileVerificationRepository
import app.thunder.api.controller.request.PostSmsRequest
import java.security.SecureRandom
import org.instancio.Instancio
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.ArgumentMatchers.anyInt
import org.springframework.context.ApplicationEventPublisher

class AuthServiceTesterCodeTest {

    private val smsClient = mock(SmsClient::class.java)
    private val mobileVerificationRepository = mock(MobileVerificationRepository::class.java)
    private val secureRandom = mock(SecureRandom::class.java).also {
        `when`(it.nextInt(anyInt())).thenAnswer { invocation ->
            if (invocation.getArgument<Int>(0) == VERIFICATION_CODE_RANGE) 0 else -1
        }
    }

    @Test
    @DisplayName("테스트 모드에서 테스터 번호로 인증번호를 요청하면 고정 인증번호 250101을 저장하고 테스트 모드로 발송한다")
    fun testerNumberInTestMode() {
        // given
        val authService = authService(testModeEnabled = true)
        val request = smsRequest(TESTER_MOBILE_NUMBER)

        // when
        authService.sendSms(request)

        // then
        verify(mobileVerificationRepository).create(request.deviceId, TESTER_MOBILE_NUMBER, request.mobileCountry, "250101")
        verify(smsClient).sendSms(TESTER_MOBILE_NUMBER, "인증번호 [250101]를 Thunder 앱에서 입력해주세요.", true)
    }

    @Test
    @DisplayName("테스트 모드에서도 테스터가 아닌 번호는 무작위 인증번호를 저장하고 테스트 모드로 발송한다")
    fun otherNumberInTestMode() {
        // given
        val authService = authService(testModeEnabled = true)
        val request = smsRequest(OTHER_MOBILE_NUMBER)

        // when
        authService.sendSms(request)

        // then
        verify(mobileVerificationRepository).create(request.deviceId, OTHER_MOBILE_NUMBER, request.mobileCountry, "100000")
        verify(smsClient).sendSms(OTHER_MOBILE_NUMBER, "인증번호 [100000]를 Thunder 앱에서 입력해주세요.", true)
    }

    @Test
    @DisplayName("테스트 모드가 꺼져 있으면 테스터 번호도 무작위 인증번호를 저장하고 실제 모드로 발송한다")
    fun testerNumberOutsideTestMode() {
        // given
        val authService = authService(testModeEnabled = false)
        val request = smsRequest(TESTER_MOBILE_NUMBER)

        // when
        authService.sendSms(request)

        // then
        verify(mobileVerificationRepository).create(request.deviceId, TESTER_MOBILE_NUMBER, request.mobileCountry, "100000")
        verify(smsClient).sendSms(TESTER_MOBILE_NUMBER, "인증번호 [100000]를 Thunder 앱에서 입력해주세요.", false)
    }

    private fun authService(testModeEnabled: Boolean) = AuthService(
        smsClient = smsClient,
        mobileVerificationRepository = mobileVerificationRepository,
        tokenManager = mock(TokenManager::class.java),
        applicationEventPublisher = mock(ApplicationEventPublisher::class.java),
        memberRepository = mock(MemberRepository::class.java),
        memberSettingRepository = mock(MemberSettingRepository::class.java),
        smsProperties = SmsProperties(testModeEnabled = testModeEnabled),
        secureRandom = secureRandom,
    )

    private fun smsRequest(mobileNumber: String) = PostSmsRequest(
        deviceId = Instancio.gen().string().alphaNumeric().length(DEVICE_ID_LENGTH).get(),
        mobileNumber = mobileNumber,
        mobileCountry = "KR",
    )

    companion object {
        private const val TESTER_MOBILE_NUMBER = "01000000000"
        private const val OTHER_MOBILE_NUMBER = "01012345678"
        private const val DEVICE_ID_LENGTH = 12
        private const val VERIFICATION_CODE_RANGE = 900_000
    }

}
