package app.thunder.api.auth

import app.thunder.libraries.errors.MemberErrors.EXPIRED_TOKEN
import app.thunder.libraries.errors.MemberErrors.INVALID_TOKEN
import app.thunder.libraries.errors.ThunderException
import java.time.Duration
import java.time.Instant
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.instancio.Instancio
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class TokenManagerTest {

    private val secretKey = Instancio.gen().string().alphaNumeric().length(SECRET_KEY_LENGTH).get()

    @Test
    @DisplayName("리프레시 토큰은 subject와 발급 시각을 담아 그대로 해석된다")
    fun refreshToken() {
        // given
        val subject = Instancio.gen().longs().range(1L, MAX_MEMBER_ID).get()
        val tokenManager = TokenManager(JwtProperties(secretKey, ACCESS_EXPIRATION_MS, REFRESH_EXPIRATION_MS))

        // when
        val token = tokenManager.generateRefreshToken(subject)

        // then
        val claims = tokenManager.parse(token)
        assertThat(claims.subject).isEqualTo(subject.toString())
        assertThat(claims.issuedAt).isBetween(Instant.now().minus(Duration.ofMinutes(1)), Instant.now())
    }

    @Test
    @DisplayName("리프레시 만료 시간이 이미 지난 리프레시 토큰을 해석하면 EXPIRED_TOKEN 예외를 던진다")
    fun expiredRefreshToken() {
        // given
        val tokenManager = TokenManager(JwtProperties(secretKey, ACCESS_EXPIRATION_MS, -REFRESH_EXPIRATION_MS))
        val token = tokenManager.generateRefreshToken(Instancio.gen().longs().range(1L, MAX_MEMBER_ID).get())

        // when
        val thrown = assertThatThrownBy { tokenManager.parse(token) }

        // then
        thrown.isInstanceOfSatisfying(ThunderException::class.java) {
            assertThat(it.errorCode).isEqualTo(EXPIRED_TOKEN)
        }
    }

    @Test
    @DisplayName("액세스 만료 시간이 이미 지난 액세스 토큰을 해석하면 EXPIRED_TOKEN 예외를 던진다")
    fun expiredAccessToken() {
        // given
        val tokenManager = TokenManager(JwtProperties(secretKey, -ACCESS_EXPIRATION_MS, REFRESH_EXPIRATION_MS))
        val token = tokenManager.generateAccessToken(Instancio.gen().longs().range(1L, MAX_MEMBER_ID).get())

        // when
        val thrown = assertThatThrownBy { tokenManager.parse(token) }

        // then
        thrown.isInstanceOfSatisfying(ThunderException::class.java) {
            assertThat(it.errorCode).isEqualTo(EXPIRED_TOKEN)
        }
    }

    @Test
    @DisplayName("다른 비밀키로 서명된 토큰을 해석하면 INVALID_TOKEN 예외를 던진다")
    fun tokenSignedWithOtherKey() {
        // given
        val issuer = TokenManager(JwtProperties(secretKey, ACCESS_EXPIRATION_MS, REFRESH_EXPIRATION_MS))
        val verifier = TokenManager(JwtProperties(secretKey.reversed() + "x", ACCESS_EXPIRATION_MS, REFRESH_EXPIRATION_MS))
        val token = issuer.generateAccessToken(Instancio.gen().longs().range(1L, MAX_MEMBER_ID).get())

        // when
        val thrown = assertThatThrownBy { verifier.parse(token) }

        // then
        thrown.isInstanceOfSatisfying(ThunderException::class.java) {
            assertThat(it.errorCode).isEqualTo(INVALID_TOKEN)
        }
    }

    companion object {
        private const val SECRET_KEY_LENGTH = 48
        private const val MAX_MEMBER_ID = 1_000_000L
        private const val ACCESS_EXPIRATION_MS = 3_600_000L
        private const val REFRESH_EXPIRATION_MS = 86_400_000L
    }

}
