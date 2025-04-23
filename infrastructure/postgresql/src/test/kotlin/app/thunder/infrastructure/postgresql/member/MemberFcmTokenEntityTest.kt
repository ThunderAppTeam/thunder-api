package app.thunder.infrastructure.postgresql.member

import app.thunder.infrastructure.postgresql.PostgresqlJpaTest
import app.thunder.infrastructure.postgresql.member.entity.MemberFcmTokenEntity
import app.thunder.infrastructure.postgresql.member.persistence.MemberFcmTokenJpaRepository
import jakarta.persistence.EntityManager
import java.time.LocalDateTime
import org.assertj.core.api.Assertions.assertThat
import org.instancio.Instancio
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@PostgresqlJpaTest
class MemberFcmTokenEntityTest {

    @Autowired
    private lateinit var jpaRepository: MemberFcmTokenJpaRepository

    @Autowired
    private lateinit var entityManager: EntityManager

    @Test
    @DisplayName("새로 만든 엔티티는 회원 ID와 토큰을 가지고 생성 시각이 기록되며 갱신 시각은 비어 있다")
    fun create() {
        // given
        val memberId = positiveId()
        val token = fcmToken()

        // when
        val entity = MemberFcmTokenEntity.create(memberId, token)

        // then
        assertThat(entity.memberId).isEqualTo(memberId)
        assertThat(entity.fcmToken).isEqualTo(token)
        assertThat(entity.createdAt).isNotNull()
        assertThat(entity.updatedAt).isNull()
    }

    @Test
    @DisplayName("엔티티의 토큰과 갱신 시각을 바꾸면 저장된 행에 반영된다")
    fun update() {
        // given
        val memberId = positiveId()
        val newToken = fcmToken()
        val updatedAt = LocalDateTime.of(2026, 1, 2, 3, 4, 5)
        val entity = jpaRepository.saveAndFlush(MemberFcmTokenEntity.create(memberId, fcmToken()))

        // when
        entity.update(newToken, updatedAt)
        entityManager.flush()
        entityManager.clear()

        // then
        val reloaded = jpaRepository.findByMemberId(memberId)!!
        assertThat(reloaded.fcmToken).isEqualTo(newToken)
        assertThat(reloaded.updatedAt).isEqualTo(updatedAt)
    }

    private fun fcmToken(): String = Instancio.gen().string().length(32).get()

    private fun positiveId(): Long = Instancio.gen().longs().min(1L).max(1_000_000L).get()

}
