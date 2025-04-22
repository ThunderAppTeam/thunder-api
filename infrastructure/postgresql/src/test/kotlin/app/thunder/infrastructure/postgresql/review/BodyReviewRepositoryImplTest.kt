package app.thunder.infrastructure.postgresql.review

import app.thunder.infrastructure.postgresql.PostgresqlJpaTest
import org.assertj.core.api.Assertions.assertThat
import org.instancio.Instancio
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate

@PostgresqlJpaTest
class BodyReviewRepositoryImplTest {

    @Autowired
    private lateinit var repository: BodyReviewRepositoryImpl

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Test
    @DisplayName("사진의 리뷰를 모두 지우면 그 사진의 리뷰만 사라진다")
    fun deleteAllByBodyPhotoId() {
        // given
        val targetPhotoId = positiveId()
        val keptPhotoId = targetPhotoId + 1
        repository.create(targetPhotoId, positiveId(), 4)
        repository.create(targetPhotoId, positiveId(), 5)
        repository.create(keptPhotoId, positiveId(), 3)

        // when
        repository.deleteAllByBodyPhotoId(targetPhotoId)

        // then
        assertThat(countByPhoto(targetPhotoId)).isEqualTo(0)
        assertThat(countByPhoto(keptPhotoId)).isEqualTo(1)
    }

    private fun countByPhoto(bodyPhotoId: Long): Int =
        jdbcTemplate.queryForObject("SELECT COUNT(*) FROM body_review WHERE body_photo_id = ?", Int::class.java, bodyPhotoId)!!

    private fun positiveId(): Long = Instancio.gen().longs().min(1L).max(1_000_000L).get()

}
