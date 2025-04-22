package app.thunder.infrastructure.postgresql.member

import app.thunder.infrastructure.postgresql.PostgresqlJpaTest
import java.time.LocalDateTime
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ClassPathResource
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.jdbc.core.JdbcTemplate

@PostgresqlJpaTest
class FcmTokenMigrationTest {

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Test
    @DisplayName("FCM 토큰 중복 제거 migration은 토큰마다 가장 최근에 갱신된 행만 남긴다")
    fun dedupeKeepsMostRecentlyUpdated() {
        // given
        dropUniqueIndex()
        insertToken(1L, "shared-a", created = BASE, updated = BASE.plusHours(5))
        insertToken(2L, "shared-a", created = BASE.plusHours(1), updated = BASE.plusHours(2))
        insertToken(3L, "shared-b", created = BASE, updated = null)
        insertToken(4L, "shared-b", created = BASE.plusHours(1), updated = null)
        insertToken(5L, "shared-c", created = BASE, updated = null)
        insertToken(6L, "shared-c", created = BASE, updated = null)
        insertToken(7L, "shared-d", created = BASE, updated = BASE.plusHours(1))
        insertToken(8L, "shared-d", created = BASE.plusHours(2), updated = null)
        insertToken(9L, "unique", created = BASE, updated = null)

        // when
        runMigration()

        // then
        assertThat(remainingMemberIds()).containsExactlyInAnyOrder(1L, 4L, 6L, 8L, 9L)
    }

    @Test
    @DisplayName("FCM 토큰 migration 뒤에는 같은 토큰을 두 회원에게 저장할 수 없다")
    fun fcmTokenUniqueIndex() {
        // given
        insertToken(1L, "token", created = BASE, updated = null)

        // when
        val duplicate = { insertToken(2L, "token", created = BASE, updated = null) }

        // then
        assertThatThrownBy(duplicate).isInstanceOf(DataIntegrityViolationException::class.java)
    }

    private fun dropUniqueIndex() {
        jdbcTemplate.execute("DROP INDEX IF EXISTS $FCM_TOKEN_UNIQUE_INDEX")
    }

    private fun runMigration() {
        val sql = ClassPathResource("db/migration/$DEDUPE_FCM_TOKEN").inputStream.use { String(it.readAllBytes()) }
        jdbcTemplate.execute(sql)
    }

    private fun insertToken(memberId: Long, token: String, created: LocalDateTime, updated: LocalDateTime?) {
        jdbcTemplate.update(
            "INSERT INTO member_fcm_token (member_id, fcm_token, created_at, updated_at) VALUES (?, ?, ?, ?)",
            memberId, token, created, updated,
        )
    }

    private fun remainingMemberIds(): List<Long> =
        jdbcTemplate.queryForList("SELECT member_id FROM member_fcm_token", Long::class.java)

    companion object {
        private val BASE = LocalDateTime.of(2026, 1, 1, 0, 0)
        private const val FCM_TOKEN_UNIQUE_INDEX = "uq_member_fcm_token_fcm_token"
        private const val DEDUPE_FCM_TOKEN = "V27__dedupe_fcm_token_and_add_unique_index.sql"
    }

}
