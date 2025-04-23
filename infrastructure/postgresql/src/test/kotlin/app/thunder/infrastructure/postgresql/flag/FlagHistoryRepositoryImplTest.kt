package app.thunder.infrastructure.postgresql.flag

import app.thunder.core.flag.domain.FlagReason
import app.thunder.infrastructure.postgresql.PostgresqlJpaTest
import org.assertj.core.api.Assertions.assertThat
import org.instancio.Instancio
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@PostgresqlJpaTest
class FlagHistoryRepositoryImplTest {

    @Autowired
    private lateinit var repository: FlagHistoryRepositoryImpl

    @Test
    @DisplayName("사진의 신고 기록을 모두 지우면 그 사진의 기록만 사라진다")
    fun deleteAllByBodyPhotoId() {
        // given
        val targetPhotoId = positiveId()
        val keptPhotoId = targetPhotoId + 1
        repository.create(1L, targetPhotoId, FlagReason.OTHER, "a")
        repository.create(2L, targetPhotoId, FlagReason.SEXUAL_CONTENT, null)
        repository.create(1L, keptPhotoId, FlagReason.OTHER, "b")

        // when
        repository.deleteAllByBodyPhotoId(targetPhotoId)

        // then
        assertThat(repository.getFlagCountByBodyPhotoId()).isEqualTo(mapOf(keptPhotoId to 1))
    }

    @Test
    @DisplayName("신고 기록을 모두 조회하면 저장한 회원, 사진, 사유, 기타 사유가 그대로 담기고 빈 기타 사유는 null이 된다")
    fun getAll() {
        // given
        val photoId = positiveId()
        repository.create(1L, photoId, FlagReason.OTHER, "reason")
        repository.create(2L, photoId, FlagReason.SEXUAL_CONTENT, "   ")

        // when
        val result = repository.getAll().filter { it.bodyPhotoId == photoId }

        // then
        assertThat(result).hasSize(2)
        assertThat(result.map { it.flagHistoryId }).allMatch { it > 0 }.doesNotHaveDuplicates()
        assertThat(result.map { it.createdAt }).doesNotContainNull()
        assertThat(result.map { Triple(it.memberId, it.flagReason, it.otherReason) })
            .containsExactlyInAnyOrder(
                Triple(1L, FlagReason.OTHER, "reason"),
                Triple(2L, FlagReason.SEXUAL_CONTENT, null),
            )
    }

    private fun positiveId(): Long = Instancio.gen().longs().min(1L).max(1_000_000L).get()

}
