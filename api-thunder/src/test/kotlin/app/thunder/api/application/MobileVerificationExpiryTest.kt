package app.thunder.api.application

import app.thunder.core.member.domain.MobileVerification
import java.time.LocalDateTime
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class MobileVerificationExpiryTest {

    private val expiredAt = LocalDateTime.of(2026, 1, 1, 12, 0, 0)
    private val verification = MobileVerification(1L, "123456", expiredAt, 0)

    @Test
    @DisplayName("만료 시각과 같은 시각에는 만료된 것으로 본다")
    fun expiredAtBoundary() {
        // given
        val now = expiredAt

        // when
        val expired = verification.isExpired(now)

        // then
        assertThat(expired).isTrue()
    }

    @Test
    @DisplayName("만료 시각 직전에는 만료되지 않은 것으로 본다")
    fun notExpiredBeforeBoundary() {
        // given
        val now = expiredAt.minusNanos(1)

        // when
        val expired = verification.isExpired(now)

        // then
        assertThat(expired).isFalse()
    }

}
