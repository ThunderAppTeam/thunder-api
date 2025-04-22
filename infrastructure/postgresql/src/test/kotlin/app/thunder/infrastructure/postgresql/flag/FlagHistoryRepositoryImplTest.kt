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

    private fun positiveId(): Long = Instancio.gen().longs().min(1L).max(1_000_000L).get()

}
