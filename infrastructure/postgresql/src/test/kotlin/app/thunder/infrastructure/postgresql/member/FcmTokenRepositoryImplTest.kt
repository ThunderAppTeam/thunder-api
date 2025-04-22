package app.thunder.infrastructure.postgresql.member

import app.thunder.infrastructure.postgresql.PostgresqlJpaTest
import org.assertj.core.api.Assertions.assertThat
import org.instancio.Instancio
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate

@PostgresqlJpaTest
class FcmTokenRepositoryImplTest {

    @Autowired
    private lateinit var repository: FcmTokenRepositoryImpl

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Test
    @DisplayName("다른 회원이 쓰던 토큰을 저장하면 그 회원의 토큰은 사라지고 새 회원만 토큰을 가진다")
    fun createOrUpdateMovesTokenToNewOwner() {
        // given
        val token = fcmToken()
        repository.createOrUpdate(PREVIOUS_OWNER_ID, token)

        // when
        repository.createOrUpdate(NEW_OWNER_ID, token)

        // then
        assertThat(repository.getByMemberId(PREVIOUS_OWNER_ID)).isNull()
        assertThat(repository.getByMemberId(NEW_OWNER_ID)).isEqualTo(token)
        assertThat(tokenRowCount(token)).isEqualTo(1)
    }

    @Test
    @DisplayName("회원이 새 토큰을 저장하면 기존 토큰을 바꾸고 다른 회원의 토큰은 건드리지 않는다")
    fun createOrUpdateReplacesOwnToken() {
        // given
        val otherToken = fcmToken()
        val oldToken = fcmToken()
        val newToken = fcmToken()
        repository.createOrUpdate(PREVIOUS_OWNER_ID, otherToken)
        repository.createOrUpdate(NEW_OWNER_ID, oldToken)

        // when
        repository.createOrUpdate(NEW_OWNER_ID, newToken)

        // then
        assertThat(repository.getByMemberId(NEW_OWNER_ID)).isEqualTo(newToken)
        assertThat(repository.getByMemberId(PREVIOUS_OWNER_ID)).isEqualTo(otherToken)
    }

    @Test
    @DisplayName("회원의 토큰을 지우면 그 회원의 토큰만 사라진다")
    fun deleteByMemberId() {
        // given
        val keptToken = fcmToken()
        repository.createOrUpdate(PREVIOUS_OWNER_ID, fcmToken())
        repository.createOrUpdate(NEW_OWNER_ID, keptToken)

        // when
        repository.deleteByMemberId(PREVIOUS_OWNER_ID)

        // then
        assertThat(repository.getByMemberId(PREVIOUS_OWNER_ID)).isNull()
        assertThat(repository.getByMemberId(NEW_OWNER_ID)).isEqualTo(keptToken)
    }

    @Test
    @DisplayName("회원의 현재 토큰과 같은 토큰으로 지우면 그 토큰이 사라진다")
    fun deleteByMemberIdAndFcmTokenRemovesMatchingToken() {
        // given
        val token = fcmToken()
        repository.createOrUpdate(PREVIOUS_OWNER_ID, token)

        // when
        repository.deleteByMemberIdAndFcmToken(PREVIOUS_OWNER_ID, token)

        // then
        assertThat(repository.getByMemberId(PREVIOUS_OWNER_ID)).isNull()
        assertThat(tokenRowCount(token)).isEqualTo(0)
    }

    @Test
    @DisplayName("회원이 이미 새 토큰을 저장했으면 옛 토큰으로 지워도 새 토큰은 남는다")
    fun deleteByMemberIdAndFcmTokenKeepsReplacedToken() {
        // given
        val oldToken = fcmToken()
        val newToken = fcmToken()
        repository.createOrUpdate(PREVIOUS_OWNER_ID, oldToken)
        repository.createOrUpdate(PREVIOUS_OWNER_ID, newToken)

        // when
        repository.deleteByMemberIdAndFcmToken(PREVIOUS_OWNER_ID, oldToken)

        // then
        assertThat(repository.getByMemberId(PREVIOUS_OWNER_ID)).isEqualTo(newToken)
        assertThat(tokenRowCount(newToken)).isEqualTo(1)
    }

    private fun fcmToken(): String = Instancio.gen().string().length(32).get()

    private fun tokenRowCount(token: String): Int = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM member_fcm_token WHERE fcm_token = ?", Int::class.java, token,
    )!!

    companion object {
        private const val PREVIOUS_OWNER_ID = 7_201L
        private const val NEW_OWNER_ID = 7_202L
    }

}
