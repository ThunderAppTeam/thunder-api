package app.thunder.api.controller.request

import jakarta.validation.Validation
import org.assertj.core.api.Assertions.assertThat
import org.instancio.Instancio
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class PostSmsVerifyRequestTest {

    private val validator = Validation.buildDefaultValidatorFactory().validator

    @Test
    @DisplayName("국가 코드가 두 글자 이하이고 나머지 값이 올바르면 검증을 통과하고 국가 코드를 그대로 가진다")
    fun validRequest() {
        // given
        val request = verifyRequest(mobileCountry = "KR")

        // when
        val violations = validator.validate(request)

        // then
        assertThat(violations).isEmpty()
        assertThat(request.mobileCountry).isEqualTo("KR")
    }

    @Test
    @DisplayName("국가 코드가 세 글자 이상이면 mobileCountry 필드가 검증에 실패한다")
    fun tooLongCountry() {
        // given
        val request = verifyRequest(mobileCountry = "KOR")

        // when
        val violations = validator.validate(request)

        // then
        assertThat(violations).singleElement().satisfies({
            assertThat(it.propertyPath.toString()).isEqualTo("mobileCountry")
        })
    }

    @Test
    @DisplayName("국가 코드가 공백이면 mobileCountry 필드가 검증에 실패한다")
    fun blankCountry() {
        // given
        val request = verifyRequest(mobileCountry = " ")

        // when
        val violations = validator.validate(request)

        // then
        assertThat(violations).singleElement().satisfies({
            assertThat(it.propertyPath.toString()).isEqualTo("mobileCountry")
        })
    }

    private fun verifyRequest(mobileCountry: String) = PostSmsVerifyRequest(
        deviceId = Instancio.gen().string().alphaNumeric().length(DEVICE_ID_LENGTH).get(),
        mobileNumber = "01012345678",
        mobileCountry = mobileCountry,
        verificationCode = Instancio.gen().string().digits().length(VERIFICATION_CODE_LENGTH).get(),
    )

    companion object {
        private const val DEVICE_ID_LENGTH = 12
        private const val VERIFICATION_CODE_LENGTH = 6
    }

}
