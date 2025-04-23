package app.thunder.infrastructure.postgresql.member

import app.thunder.infrastructure.postgresql.PostgresqlJpaTest
import app.thunder.infrastructure.postgresql.member.persistence.MemberBlockRelationJpaRepository
import org.assertj.core.api.Assertions.assertThat
import org.instancio.Instancio
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate

@PostgresqlJpaTest
class MemberBlockRelationRepositoryImplTest {

    @Autowired
    private lateinit var repository: MemberBlockRelationRepositoryImpl

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var jpaRepository: MemberBlockRelationJpaRepository

    @Test
    @DisplayName("차단 관계를 저장하면 차단한 회원의 차단 대상 회원 ID 목록으로 조회된다")
    fun getBlockedMemberIdsByMemberId() {
        // given
        val memberId = positiveId()
        val blockedMemberIds = generateSequence { positiveId() }
            .filter { it != memberId }
            .distinct()
            .take(3)
            .toSet()
        blockedMemberIds.forEach { repository.create(memberId, it, memberId) }

        // when
        val result = repository.getBlockedMemberIdsByMemberId(memberId)

        // then
        assertThat(result).isEqualTo(blockedMemberIds)
    }

    @Test
    @DisplayName("회원의 차단 관계를 지우면 회원이 차단한 행과 회원을 차단한 행이 모두 사라지고 다른 행은 남는다")
    fun deleteAllByMemberId() {
        // given
        val (memberId, other, third, fourth) = distinctIds(4)
        repository.create(memberId, other, memberId)
        repository.create(other, memberId, memberId)
        repository.create(third, memberId, third)
        repository.create(memberId, fourth, memberId)
        repository.create(other, third, other)

        // when
        repository.deleteAllByMemberId(memberId)

        // then
        assertThat(repository.getBlockedMemberIdsByMemberId(memberId)).isEmpty()
        assertThat(repository.getBlockedMemberIdsByMemberId(other)).containsExactly(third)
        assertThat(repository.getBlockedMemberIdsByMemberId(third)).isEmpty()
        assertThat(relationCountReferencing(memberId)).isEqualTo(0)
    }

    @Test
    @DisplayName("차단 관계를 저장하면 식별자가 발급되고 차단한 회원, 차단 대상, 생성자, 생성 시각이 기록된다")
    fun createPersistsRelation() {
        // given
        val (memberId, blockedMemberId, createdBy) = distinctIds(3)

        // when
        repository.create(memberId, blockedMemberId, createdBy)

        // then
        val relation = jpaRepository.findAllByMemberId(memberId).single()
        assertThat(relation.memberBlockRelationId).isPositive()
        assertThat(relation.memberId).isEqualTo(memberId)
        assertThat(relation.blockedMemberId).isEqualTo(blockedMemberId)
        assertThat(relation.createdBy).isEqualTo(createdBy)
        assertThat(relation.createdAt).isNotNull()
    }

    @Test
    @DisplayName("이미 저장된 차단 관계를 다시 저장하면 새 행이 만들어지지 않는다")
    fun createIsIdempotent() {
        // given
        val (memberId, blockedMemberId) = distinctIds(2)
        repository.create(memberId, blockedMemberId, memberId)

        // when
        repository.create(memberId, blockedMemberId, memberId)

        // then
        assertThat(relationCountReferencing(memberId)).isEqualTo(1)
    }

    private fun relationCountReferencing(memberId: Long): Int = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM member_block_relation WHERE member_id = ? OR blocked_member_id = ?",
        Int::class.java, memberId, memberId,
    )!!

    private fun distinctIds(size: Int): List<Long> =
        generateSequence { positiveId() }.distinct().take(size).toList()

    private fun positiveId(): Long =
        Instancio.gen().longs().min(1L).max(1_000_000L).get()

}
