package app.thunder.infrastructure.postgresql.member

import app.thunder.infrastructure.postgresql.PostgresqlJpaTest
import app.thunder.infrastructure.postgresql.member.entity.MobileVerificationEntity
import java.time.LocalDateTime
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate

@PostgresqlJpaTest
class MobileVerificationRepositoryImplTest {

    @Autowired
    private lateinit var repository: MobileVerificationRepositoryImpl

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Test
    @DisplayName("휴대폰 번호의 최근 미인증 발송 수는 기기와 상관없이 센다")
    fun countByMobileNumber() {
        // given
        repository.create("device-1", MOBILE_NUMBER, "KR", "111111")
        repository.create("device-2", MOBILE_NUMBER, "KR", "222222")
        repository.create("device-3", "01099999999", "KR", "333333")

        // when
        val count = repository.getCountByMobileNumberAndCreatedAtAfter(MOBILE_NUMBER, LocalDateTime.now().minusDays(1))

        // then
        assertThat(count).isEqualTo(2)
    }

    @Test
    @DisplayName("휴대폰 번호의 발송 수에는 인증이 끝난 발송과 기간이 지난 발송을 세지 않는다")
    fun countByMobileNumberIgnoresVerifiedAndOld() {
        // given
        repository.create("device-1", MOBILE_NUMBER, "KR", "111111")
        repository.create("device-2", MOBILE_NUMBER, "KR", "222222")
        repository.create("device-3", MOBILE_NUMBER, "KR", "333333")
        jdbcTemplate.update("UPDATE mobile_verification SET verified_at = now() WHERE device_id = 'device-1'")
        jdbcTemplate.update("UPDATE mobile_verification SET created_at = now() - interval '2 days' WHERE device_id = 'device-2'")

        // when
        val count = repository.getCountByMobileNumberAndCreatedAtAfter(MOBILE_NUMBER, LocalDateTime.now().minusDays(1))

        // then
        assertThat(count).isEqualTo(1)
    }

    @Test
    @DisplayName("발송 한도 잠금은 같은 트랜잭션 안에서 반복해서 잡아도 막히지 않는다")
    fun lockSendLimitIsReentrant() {
        // given
        val deviceId = "device-1"

        // when
        repository.lockSendLimit(deviceId, MOBILE_NUMBER)
        repository.lockSendLimit(deviceId, MOBILE_NUMBER)

        // then
        assertThat(advisoryLockCount()).isEqualTo(2)
    }

    @Test
    @DisplayName("인증은 한 번만 성공하고 두 번째부터는 false를 반환한다")
    fun verifyOnlyOnce() {
        // given
        repository.create("device-1", MOBILE_NUMBER, "KR", "111111")
        val id = lastVerificationId()

        // when
        val first = repository.verify(id)
        val second = repository.verify(id)

        // then
        assertThat(first).isTrue()
        assertThat(second).isFalse()
    }

    @Test
    @DisplayName("실패 횟수가 한도에 도달한 인증은 verify가 false를 반환한다")
    fun verifyRejectedWhenLocked() {
        // given
        repository.create("device-1", MOBILE_NUMBER, "KR", "111111")
        val id = lastVerificationId()
        repeat(5) { repository.increaseFailedAttemptCount(id) }

        // when
        val verified = repository.verify(id)

        // then
        assertThat(verified).isFalse()
    }

    @Test
    @DisplayName("실패 횟수를 늘리면 조회한 인증에 반영된다")
    fun increaseFailedAttemptCount() {
        // given
        repository.create("device-1", MOBILE_NUMBER, "KR", "111111")
        val id = lastVerificationId()

        // when
        repository.increaseFailedAttemptCount(id)
        repository.increaseFailedAttemptCount(id)

        // then
        val verification = repository.getLastByDeviceIdAndMobileNumber("device-1", MOBILE_NUMBER)
        assertThat(verification?.failedAttemptCount).isEqualTo(2)
    }

    @Test
    @DisplayName("기준 시각 이후에 인증된 기록이 있는 번호만 인증된 번호로 확인된다")
    fun existsVerifiedAfter() {
        // given
        repository.create("device-1", MOBILE_NUMBER, "KR", "111111")
        repository.verify(lastVerificationId())
        repository.create("device-2", "01099999999", "KR", "222222")

        // when
        val verified = repository.existsVerifiedByMobileNumberAfter(MOBILE_NUMBER, LocalDateTime.now().minusMinutes(30))
        val verifiedLater = repository.existsVerifiedByMobileNumberAfter(MOBILE_NUMBER, LocalDateTime.now().plusMinutes(1))
        val unverified = repository.existsVerifiedByMobileNumberAfter("01099999999", LocalDateTime.now().minusMinutes(30))

        // then
        assertThat(verified).isTrue()
        assertThat(verifiedLater).isFalse()
        assertThat(unverified).isFalse()
    }

    @Test
    @DisplayName("만료 시각과 같은 시각의 엔티티는 만료된 것으로 본다")
    fun entityExpiredAtBoundary() {
        // given
        val entity = MobileVerificationEntity.create("device-1", MOBILE_NUMBER, "KR", "111111")

        // when
        val expired = entity.isExpired(entity.expiredAt)

        // then
        assertThat(expired).isTrue()
    }

    @Test
    @DisplayName("만료 시각 이전의 엔티티는 만료되지 않았고 만료 시각은 생성 3분 뒤이다")
    fun entityNotExpiredBeforeExpiry() {
        // given
        val entity = MobileVerificationEntity.create("device-1", MOBILE_NUMBER, "KR", "111111")

        // when
        val beforeExpiry = entity.isExpired(entity.expiredAt.minusNanos(1))

        // then
        assertThat(beforeExpiry).isFalse()
        assertThat(entity.expiredAt).isEqualTo(entity.createdAt.plusMinutes(3))
    }

    @Test
    @DisplayName("기준 시각을 생략하면 현재 시각으로 판단해 방금 만든 엔티티는 만료되지 않았다")
    fun entityNotExpiredWithoutReferenceTime() {
        // given
        val entity = MobileVerificationEntity.create("device-1", MOBILE_NUMBER, "KR", "111111")

        // when
        val now = entity.isExpired()

        // then
        assertThat(now).isFalse()
    }

    @Test
    @DisplayName("엔티티를 초기화하면 인증 시각이 초기값으로 기록된다")
    fun entityReset() {
        // given
        val entity = MobileVerificationEntity.create("device-1", MOBILE_NUMBER, "KR", "111111")

        // when
        entity.reset()

        // then
        assertThat(entity.verifiedAt).isEqualTo(LocalDateTime.of(1, 1, 1, 0, 0))
    }

    @Test
    @DisplayName("mobile_verification 테이블에는 mobile_number 단독 index가 있다")
    fun mobileNumberIndexExists() {
        // given
        val sql = """
            SELECT COUNT(*) FROM pg_indexes
            WHERE tablename = 'mobile_verification' AND indexdef LIKE '%(mobile_number)'
        """.trimIndent()

        // when
        val count = jdbcTemplate.queryForObject(sql, Int::class.java)

        // then
        assertThat(count).isEqualTo(1)
    }

    private fun lastVerificationId(): Long =
        jdbcTemplate.queryForObject("SELECT MAX(mobile_verification_id) FROM mobile_verification", Long::class.java)!!

    private fun advisoryLockCount(): Int =
        jdbcTemplate.queryForObject("SELECT COUNT(*) FROM pg_locks WHERE locktype = 'advisory' AND pid = pg_backend_pid()", Int::class.java)!!

    companion object {
        private const val MOBILE_NUMBER = "01012345678"
    }

}
