package app.thunder.api.photo

import app.thunder.core.photo.domain.BodyPhoto
import java.time.LocalDateTime
import org.assertj.core.api.Assertions.assertThat
import org.instancio.Instancio
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class BodyPhotoDomainScoreTest {

    @Test
    @DisplayName("리뷰가 없으면 평균 점수는 0이다")
    fun zeroReviewsAverage() {
        // given
        val bodyPhoto = bodyPhoto(reviewCount = 0, totalReviewScore = 0.0)

        // when
        val average = bodyPhoto.averageScore()

        // then
        assertThat(average).isEqualTo(0.0)
    }

    @Test
    @DisplayName("리뷰가 없으면 환산 점수는 0이다")
    fun zeroReviewsResultScore() {
        // given
        val bodyPhoto = bodyPhoto(reviewCount = 0, totalReviewScore = 0.0)

        // when
        val resultScore = bodyPhoto.getResultReviewScore()

        // then
        assertThat(resultScore).isEqualTo(0.0)
    }

    @Test
    @DisplayName("리뷰 수가 0 이하이면 합계 점수가 남아 있어도 평균 점수를 0으로 계산한다")
    fun nonPositiveReviewsAverage() {
        // given
        val bodyPhoto = bodyPhoto(reviewCount = -1, totalReviewScore = 5.0)

        // when
        val average = bodyPhoto.averageScore()

        // then
        assertThat(average).isEqualTo(0.0)
    }

    @Test
    @DisplayName("리뷰 수가 0 이하이면 합계 점수가 남아 있어도 환산 점수를 0으로 계산한다")
    fun nonPositiveReviewsResultScore() {
        // given
        val bodyPhoto = bodyPhoto(reviewCount = -1, totalReviewScore = 5.0)

        // when
        val resultScore = bodyPhoto.getResultReviewScore()

        // then
        assertThat(resultScore).isEqualTo(0.0)
    }

    @Test
    @DisplayName("리뷰가 있으면 평균 점수는 합계 점수를 리뷰 수로 나눈 값이다")
    fun reviewedAverage() {
        // given
        val bodyPhoto = bodyPhoto(reviewCount = 3, totalReviewScore = 10.0)

        // when
        val average = bodyPhoto.averageScore()

        // then
        assertThat(average).isEqualTo(10.0 / 3)
    }

    @Test
    @DisplayName("리뷰가 있으면 평균 점수를 10점 만점으로 환산해 소수 첫째 자리까지 반올림한다")
    fun reviewedResultScore() {
        // given
        val bodyPhoto = bodyPhoto(reviewCount = 3, totalReviewScore = 10.0)

        // when
        val resultScore = bodyPhoto.getResultReviewScore()

        // then
        assertThat(resultScore).isEqualTo(6.7)
    }

    private fun bodyPhoto(reviewCount: Int, totalReviewScore: Double) = BodyPhoto(
        bodyPhotoId = Instancio.create(Long::class.javaObjectType),
        memberId = Instancio.create(Long::class.javaObjectType),
        imageUrl = Instancio.create(String::class.java),
        reviewCount = reviewCount,
        totalReviewScore = totalReviewScore,
        createdAt = LocalDateTime.now(),
        updatedAt = null,
    )

}
