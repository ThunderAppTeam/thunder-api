package app.thunder.infrastructure.postgresql.review

import app.thunder.core.review.command.CreateReviewableBodyPhotoCommand
import app.thunder.infrastructure.postgresql.PostgresqlJpaTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate

@PostgresqlJpaTest
class ReviewableBodyPhotoRepositoryImplTest {

    @Autowired
    private lateinit var repository: ReviewableBodyPhotoRepositoryImpl

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Test
    @DisplayName("회원의 큐에 있는 사진만 존재한다고 답한다")
    fun exists() {
        // given
        repository.saveAll(listOf(command(MEMBER_ID, 1L, OWNER_ID)))

        // when
        val queued = repository.exists(MEMBER_ID, 1L)
        val otherPhoto = repository.exists(MEMBER_ID, 2L)
        val otherMember = repository.exists(OTHER_MEMBER_ID, 1L)

        // then
        assertThat(queued).isTrue()
        assertThat(otherPhoto).isFalse()
        assertThat(otherMember).isFalse()
    }

    @Test
    @DisplayName("같은 사진을 두 번 저장해도 큐에는 한 번만 들어간다")
    fun saveAllSkipsDuplicates() {
        // given
        val commands = listOf(command(MEMBER_ID, 1L, OWNER_ID), command(MEMBER_ID, 1L, OWNER_ID))

        // when
        repository.saveAll(commands)

        // then
        assertThat(queueSize(MEMBER_ID)).isEqualTo(1)
    }

    @Test
    @DisplayName("사진 id로 큐를 지우면 그 사진이 담긴 모든 회원의 행만 사라진다")
    fun deleteAllByBodyPhotoId() {
        // given
        repository.saveAll(
            listOf(command(MEMBER_ID, 1L, OWNER_ID), command(OTHER_MEMBER_ID, 1L, OWNER_ID), command(MEMBER_ID, 2L, OWNER_ID)),
        )

        // when
        repository.deleteAllByBodyPhotoId(1L)

        // then
        assertThat(queueSize(MEMBER_ID)).isEqualTo(1)
        assertThat(queueSize(OTHER_MEMBER_ID)).isEqualTo(0)
    }

    @Test
    @DisplayName("회원 id로 큐를 지우면 회원 자신의 큐와 회원 사진이 담긴 큐가 모두 사라진다")
    fun deleteAllByMemberId() {
        // given
        repository.saveAll(
            listOf(command(MEMBER_ID, 1L, OWNER_ID), command(OTHER_MEMBER_ID, 2L, MEMBER_ID), command(OTHER_MEMBER_ID, 3L, OWNER_ID)),
        )

        // when
        repository.deleteAllByMemberId(MEMBER_ID)

        // then
        assertThat(queueSize(MEMBER_ID)).isEqualTo(0)
        assertThat(repository.getAllByMemberId(OTHER_MEMBER_ID).map { it.bodyPhotoId }).containsExactly(3L)
    }

    @Test
    @DisplayName("회원과 사진 주인으로 큐를 지우면 그 사진 주인의 행만 사라진다")
    fun deleteAllByMemberIdAndPhotoMemberId() {
        // given
        repository.saveAll(
            listOf(command(MEMBER_ID, 1L, OWNER_ID), command(MEMBER_ID, 2L, OTHER_MEMBER_ID), command(OTHER_MEMBER_ID, 3L, OWNER_ID)),
        )

        // when
        repository.deleteAllByMemberIdAndPhotoMemberId(MEMBER_ID, OWNER_ID)

        // then
        assertThat(repository.getAllByMemberId(MEMBER_ID).map { it.bodyPhotoId }).containsExactly(2L)
        assertThat(queueSize(OTHER_MEMBER_ID)).isEqualTo(1)
    }

    @Test
    @DisplayName("큐 잠금은 트랜잭션 안에서 어드바이저리 락을 잡고 같은 트랜잭션에서 다시 잡아도 막히지 않는다")
    fun lockQueueIsReentrant() {
        // given
        val before = advisoryLockCount()

        // when
        repository.lockQueue(MEMBER_ID)
        repository.lockQueue(MEMBER_ID)

        // then
        assertThat(advisoryLockCount() - before).isEqualTo(1)
    }

    @Test
    @DisplayName("회원 id 집합이 비어 있으면 큐에 행이 있어도 빈 목록을 돌려준다")
    fun getFirstByMemberIdsWithEmptySet() {
        // given
        repository.saveAll(listOf(command(MEMBER_ID, 1L, OWNER_ID), command(OTHER_MEMBER_ID, 2L, OWNER_ID)))

        // when
        val result = repository.getFirstByMemberIds(emptySet())

        // then
        assertThat(result).isEmpty()
    }

    @Test
    @DisplayName("요청한 회원들의 큐가 비어 있으면 다른 회원의 행을 돌려주지 않는다")
    fun getFirstByMemberIdsWithoutMatchingRows() {
        // given
        repository.saveAll(listOf(command(OTHER_MEMBER_ID, 2L, OWNER_ID)))

        // when
        val result = repository.getFirstByMemberIds(setOf(MEMBER_ID))

        // then
        assertThat(result).isEmpty()
    }

    private fun command(memberId: Long, bodyPhotoId: Long, bodyPhotoMemberId: Long) =
        CreateReviewableBodyPhotoCommand(memberId, bodyPhotoId, bodyPhotoMemberId)

    private fun queueSize(memberId: Long): Int = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM reviewable_body_photo WHERE member_id = ?", Int::class.java, memberId,
    )!!

    private fun advisoryLockCount(): Int = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM pg_locks WHERE locktype = 'advisory' AND pid = pg_backend_pid()", Int::class.java,
    )!!

    companion object {
        private const val MEMBER_ID = 7_101L
        private const val OTHER_MEMBER_ID = 7_102L
        private const val OWNER_ID = 7_103L
    }

}
