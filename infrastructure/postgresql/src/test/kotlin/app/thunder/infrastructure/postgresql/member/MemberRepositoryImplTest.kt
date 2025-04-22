package app.thunder.infrastructure.postgresql.member

import app.thunder.infrastructure.postgresql.PostgresqlJpaTest
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import org.assertj.core.api.Assertions.assertThat
import org.instancio.Instancio
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@PostgresqlJpaTest
class MemberRepositoryImplTest {

    @Autowired
    private lateinit var repository: MemberRepositoryImpl

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Test
    @DisplayName("로그아웃 시각이 비어 있으면 요청한 시각으로 토큰 폐기 시점을 정한다")
    fun revokeTokensWhenCutoffIsNull() {
        // given
        val memberId = insertMember(loggedOutAt = null)
        val revokedAt = BASE_TIME

        // when
        repository.revokeTokensIssuedBefore(memberId, revokedAt)

        // then
        assertThat(loggedOutAt(memberId)).isEqualTo(revokedAt.truncatedTo(ChronoUnit.MICROS))
    }

    @Test
    @DisplayName("저장된 폐기 시점이 더 늦으면 더 이른 시각으로 요청해도 폐기 시점을 되돌리지 않는다")
    fun revokeTokensDoesNotMoveCutoffBackward() {
        // given
        val storedCutoff = BASE_TIME.plusSeconds(10)
        val memberId = insertMember(loggedOutAt = storedCutoff)

        // when
        repository.revokeTokensIssuedBefore(memberId, BASE_TIME)

        // then
        assertThat(loggedOutAt(memberId)).isEqualTo(storedCutoff.truncatedTo(ChronoUnit.MICROS))
    }

    @Test
    @DisplayName("저장된 폐기 시점이 더 이르면 더 늦은 시각으로 폐기 시점을 앞으로 옮긴다")
    fun revokeTokensMovesCutoffForward() {
        // given
        val memberId = insertMember(loggedOutAt = BASE_TIME)
        val revokedAt = BASE_TIME.plusSeconds(10)

        // when
        repository.revokeTokensIssuedBefore(memberId, revokedAt)

        // then
        assertThat(loggedOutAt(memberId)).isEqualTo(revokedAt.truncatedTo(ChronoUnit.MICROS))
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("서로 다른 시각으로 동시에 폐기를 요청해도 가장 늦은 시각이 최종 폐기 시점으로 남는다")
    fun concurrentRevocationsKeepLatestCutoff() {
        // given
        val memberId = insertMember(loggedOutAt = null)
        val revokedTimes = List(CONCURRENT_WRITERS) { BASE_TIME.plusSeconds(it.toLong()) }
        val executor = Executors.newFixedThreadPool(CONCURRENT_WRITERS)
        val ready = CountDownLatch(CONCURRENT_WRITERS)
        val start = CountDownLatch(1)

        try {
            val futures = revokedTimes.map { revokedAt ->
                executor.submit {
                    ready.countDown()
                    start.await()
                    repository.revokeTokensIssuedBefore(memberId, revokedAt)
                }
            }
            ready.await()

            // when
            start.countDown()
            futures.forEach { it.get() }

            // then
            assertThat(loggedOutAt(memberId)).isEqualTo(revokedTimes.max().truncatedTo(ChronoUnit.MICROS))
        } finally {
            executor.shutdown()
            jdbcTemplate.update("DELETE FROM member WHERE member_id = ?", memberId)
        }
    }

    private fun insertMember(loggedOutAt: LocalDateTime?): Long {
        val memberId = Instancio.gen().longs().min(1L).max(1_000_000L).get()
        jdbcTemplate.update(
            """
            INSERT INTO member (member_id, nickname, mobile_number, mobile_country, gender, birth_day,
                                country_code, marketing_agreement, member_uuid, logged_out_at, created_at)
            OVERRIDING SYSTEM VALUE
            VALUES (?, ?, ?, 'KR', 'MALE', DATE '1990-01-01', 'KR', FALSE, ?, ?, ?)
            """.trimIndent(),
            memberId, "user$memberId", "010$memberId", UUID.randomUUID(), loggedOutAt, BASE_TIME,
        )
        return memberId
    }

    private fun loggedOutAt(memberId: Long): LocalDateTime? =
        jdbcTemplate.queryForObject(
            "SELECT logged_out_at FROM member WHERE member_id = ?",
            LocalDateTime::class.java,
            memberId,
        )

    companion object {
        private const val CONCURRENT_WRITERS = 8
        private val BASE_TIME: LocalDateTime = LocalDateTime.of(2030, 1, 1, 0, 0, 0, 123_456_000)
    }

}
