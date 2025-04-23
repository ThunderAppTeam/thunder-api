package app.thunder.infrastructure.postgresql.photo

import app.thunder.core.member.domain.Gender
import app.thunder.core.photo.domain.BodyPhoto
import jakarta.persistence.EntityManager
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.instancio.Instancio
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import app.thunder.infrastructure.postgresql.PostgresqlJpaTest

@PostgresqlJpaTest
class BodyPhotoRepositoryImplTest {

    @Autowired
    private lateinit var repository: BodyPhotoRepositoryImpl

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var entityManager: EntityManager

    private var femaleId = 0L
    private var otherFemaleId = 0L
    private var maleId = 0L

    @BeforeEach
    fun createMembers() {
        femaleId = insertMember(Gender.FEMALE)
        otherFemaleId = insertMember(Gender.FEMALE)
        maleId = insertMember(Gender.MALE)
    }

    @Test
    @DisplayName("리뷰가 완료된 사진 수는 같은 성별의 기준 시각 이후 사진만 센다")
    fun countReviewCompletedByGender() {
        // given
        val since = LocalDateTime.now().minusDays(30)
        insertPhoto(femaleId, reviewCount = 10, totalReviewScore = 40.0)
        insertPhoto(otherFemaleId, reviewCount = 10, totalReviewScore = 30.0)
        insertPhoto(femaleId, reviewCount = 10, totalReviewScore = 40.0, createdAt = since.minusMinutes(1))
        insertPhoto(maleId, reviewCount = 10, totalReviewScore = 40.0)

        // when
        val count = repository.countReviewCompletedByGender(Gender.FEMALE, since)

        // then
        assertThat(count).isEqualTo(2)
    }

    @Test
    @DisplayName("리뷰가 완료되지 않은 사진은 리뷰가 완료된 사진 수에 넣지 않는다")
    fun incompletePhotoIsNotCounted() {
        // given
        val since = LocalDateTime.now().minusDays(30)
        insertPhoto(femaleId, reviewCount = 10, totalReviewScore = 40.0)
        insertPhoto(femaleId, reviewCount = 9, totalReviewScore = 45.0)
        insertPhoto(otherFemaleId, reviewCount = 0, totalReviewScore = 0.0)

        // when
        val count = repository.countReviewCompletedByGender(Gender.FEMALE, since)

        // then
        assertThat(count).isEqualTo(1)
    }

    @Test
    @DisplayName("평균 점수가 더 높은 사진 수는 같은 성별의 기준 시각 이후 리뷰 완료 사진 중에서만 센다")
    fun countHigherAverageWithinCompletedCohort() {
        // given
        val since = LocalDateTime.now().minusDays(30)
        insertPhoto(femaleId, reviewCount = 10, totalReviewScore = 50.0)
        insertPhoto(otherFemaleId, reviewCount = 10, totalReviewScore = 45.0)
        insertPhoto(femaleId, reviewCount = 10, totalReviewScore = 50.0, createdAt = since.minusMinutes(1))
        insertPhoto(maleId, reviewCount = 10, totalReviewScore = 50.0)

        // when
        val count = repository.countReviewCompletedByGenderWithHigherAverageScore(Gender.FEMALE, 4.0, since)

        // then
        assertThat(count).isEqualTo(2)
    }

    @Test
    @DisplayName("평균 점수가 같은 사진은 더 높은 것으로 세지 않는다")
    fun equalAverageIsNotHigher() {
        // given
        val since = LocalDateTime.now().minusDays(30)
        insertPhoto(femaleId, reviewCount = 10, totalReviewScore = 40.0)
        insertPhoto(otherFemaleId, reviewCount = 10, totalReviewScore = 40.0)

        // when
        val count = repository.countReviewCompletedByGenderWithHigherAverageScore(Gender.FEMALE, 4.0, since)

        // then
        assertThat(count).isEqualTo(0)
    }

    @Test
    @DisplayName("리뷰가 완료되지 않은 사진은 평균 점수가 더 높아도 세지 않는다")
    fun incompletePhotoIsNotCountedEvenWithHigherAverage() {
        // given
        val since = LocalDateTime.now().minusDays(30)
        insertPhoto(femaleId, reviewCount = 1, totalReviewScore = 5.0)
        insertPhoto(otherFemaleId, reviewCount = 9, totalReviewScore = 45.0)

        // when
        val count = repository.countReviewCompletedByGenderWithHigherAverageScore(Gender.FEMALE, 4.0, since)

        // then
        assertThat(count).isEqualTo(0)
    }

    @Test
    @DisplayName("생성 시각이 기준 시각과 같은 리뷰 완료 사진도 리뷰가 완료된 사진 수에 센다")
    fun countIncludesPhotoCreatedExactlyAtSince() {
        // given
        val since = LocalDateTime.now().minusDays(30).truncatedTo(ChronoUnit.MICROS)
        insertPhoto(femaleId, reviewCount = 10, totalReviewScore = 40.0, createdAt = since)
        insertPhoto(otherFemaleId, reviewCount = 10, totalReviewScore = 40.0, createdAt = since.minus(1, ChronoUnit.MICROS))

        // when
        val count = repository.countReviewCompletedByGender(Gender.FEMALE, since)

        // then
        assertThat(count).isEqualTo(1)
    }

    @Test
    @DisplayName("생성 시각이 기준 시각과 같은 리뷰 완료 사진도 평균 점수가 더 높으면 센다")
    fun countHigherIncludesPhotoCreatedExactlyAtSince() {
        // given
        val since = LocalDateTime.now().minusDays(30).truncatedTo(ChronoUnit.MICROS)
        insertPhoto(femaleId, reviewCount = 10, totalReviewScore = 50.0, createdAt = since)
        insertPhoto(otherFemaleId, reviewCount = 10, totalReviewScore = 50.0, createdAt = since.minus(1, ChronoUnit.MICROS))

        // when
        val count = repository.countReviewCompletedByGenderWithHigherAverageScore(Gender.FEMALE, 4.0, since)

        // then
        assertThat(count).isEqualTo(1)
    }

    @Test
    @DisplayName("회원의 사진 목록은 최신 사진부터 조회된다")
    fun getAllByMemberId() {
        // given
        val first = insertPhoto(femaleId)
        val second = insertPhoto(femaleId)
        insertPhoto(maleId)

        // when
        val result = repository.getAllByMemberId(femaleId)

        // then
        assertThat(result.map { it.bodyPhotoId }).containsExactly(second, first)
    }

    @Test
    @DisplayName("리뷰가 끝나지 않은 사진만 조회된다")
    fun getNotReviewCompletedAll() {
        // given
        val incomplete = insertPhoto(femaleId, reviewCount = 9)
        insertPhoto(femaleId, reviewCount = 10)

        // when
        val result = repository.getNotReviewCompletedAll()

        // then
        assertThat(result.map { it.bodyPhotoId }).containsExactly(incomplete)
    }

    @Test
    @DisplayName("리뷰를 더하면 리뷰 수와 점수 합계가 하나의 갱신으로 늘고 갱신 시각이 기록된다")
    fun addReview() {
        // given
        val photoId = insertPhoto(femaleId, reviewCount = 2, totalReviewScore = 7.0)

        // when
        val changed = repository.addReview(photoId, 4)

        // then
        val photo = repository.getById(photoId)!!
        assertThat(changed).isTrue()
        assertThat(photo.reviewCount).isEqualTo(3)
        assertThat(photo.totalReviewScore).isEqualTo(11.0)
        assertThat(photo.updatedAt).isNotNull()
    }

    @Test
    @DisplayName("리뷰가 이미 10개인 사진에는 리뷰를 더하지 않고 false를 돌려준다")
    fun addReviewToCompletedPhoto() {
        // given
        val photoId = insertPhoto(femaleId, reviewCount = 10, totalReviewScore = 40.0)

        // when
        val changed = repository.addReview(photoId, 5)

        // then
        val photo = repository.getById(photoId)!!
        assertThat(changed).isFalse()
        assertThat(photo.reviewCount).isEqualTo(10)
        assertThat(photo.totalReviewScore).isEqualTo(40.0)
    }

    @Test
    @DisplayName("없는 사진에 리뷰를 더하면 false를 돌려준다")
    fun addReviewToMissingPhoto() {
        // given
        val missingId = 987_654L

        // when
        val changed = repository.addReview(missingId, 3)

        // then
        assertThat(changed).isFalse()
    }

    @Test
    @DisplayName("사진을 수정하면 리뷰 수와 점수 합계, 갱신 시각이 저장되고 다른 사진은 그대로이다")
    fun update() {
        // given
        val photoId = insertPhoto(femaleId, reviewCount = 2, totalReviewScore = 7.0)
        val otherPhotoId = insertPhoto(femaleId, reviewCount = 3, totalReviewScore = 9.0)
        val updatedAt = LocalDateTime.of(2026, 1, 2, 3, 4, 5)
        val stored = repository.getById(photoId)!!
        val changed = BodyPhoto(
            bodyPhotoId = stored.bodyPhotoId,
            memberId = stored.memberId,
            imageUrl = stored.imageUrl,
            reviewCount = 7,
            totalReviewScore = 28.5,
            createdAt = stored.createdAt,
            updatedAt = updatedAt,
        )

        // when
        repository.update(changed)
        entityManager.flush()
        entityManager.clear()

        // then
        val photo = repository.getById(photoId)!!
        assertThat(photo.reviewCount).isEqualTo(7)
        assertThat(photo.totalReviewScore).isEqualTo(28.5)
        assertThat(photo.updatedAt).isEqualTo(updatedAt)
        val other = repository.getById(otherPhotoId)!!
        assertThat(other.reviewCount).isEqualTo(3)
        assertThat(other.totalReviewScore).isEqualTo(9.0)
        assertThat(other.updatedAt).isNull()
    }

    @Test
    @DisplayName("없는 사진을 수정해도 새 사진이 만들어지지 않는다")
    fun updateMissingPhoto() {
        // given
        val missingId = 987_654L
        val missing = BodyPhoto(
            bodyPhotoId = missingId,
            memberId = femaleId,
            imageUrl = Instancio.gen().net().uri().get().toString(),
            reviewCount = 5,
            totalReviewScore = 20.0,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
        )

        // when
        repository.update(missing)
        entityManager.flush()
        entityManager.clear()

        // then
        assertThat(repository.getById(missingId)).isNull()
    }

    private fun insertMember(gender: Gender): Long {
        val suffix = Instancio.gen().string().digits().length(10).get()
        return jdbcTemplate.queryForObject(
            """
            INSERT INTO member (nickname, mobile_number, mobile_country, gender, birth_day, country_code,
                                marketing_agreement, member_uuid, created_at)
            VALUES (?, ?, 'KR', ?, DATE '1990-01-01', 'KR', FALSE, ?, ?) RETURNING member_id
            """.trimIndent(),
            Long::class.java, "nick$suffix", "010$suffix", gender.name, UUID.randomUUID(), LocalDateTime.now(),
        )!!
    }

    private fun insertPhoto(
        memberId: Long,
        reviewCount: Int = 0,
        totalReviewScore: Double = 0.0,
        createdAt: LocalDateTime = LocalDateTime.now(),
    ): Long = jdbcTemplate.queryForObject(
        """
        INSERT INTO body_photo (member_id, image_url, review_count, total_review_score, created_at)
        VALUES (?, ?, ?, ?, ?) RETURNING body_photo_id
        """.trimIndent(),
        Long::class.java, memberId, "https://bucket/${UUID.randomUUID()}.jpg", reviewCount, totalReviewScore, createdAt,
    )!!

}
