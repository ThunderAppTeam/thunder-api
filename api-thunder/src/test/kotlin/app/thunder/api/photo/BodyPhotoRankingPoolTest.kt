package app.thunder.api.photo

import app.thunder.core.photo.domain.BodyPhoto
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class BodyPhotoRankingPoolTest {

    private val since: LocalDateTime = LocalDateTime.of(2026, 1, 1, 0, 0, 0)

    @Test
    @DisplayName("리뷰가 완료되고 생성 시각이 기준 시각과 같으면 순위 집계 대상이다")
    fun completedAtSinceIsInPool() {
        // given
        val photo = bodyPhoto(reviewCount = 10, createdAt = since)

        // when
        val inPool = photo.isInRankingPool(since)

        // then
        assertTrue(inPool)
    }

    @Test
    @DisplayName("리뷰가 완료되어도 생성 시각이 기준 시각보다 1마이크로초 이르면 순위 집계 대상이 아니다")
    fun completedBeforeSinceIsNotInPool() {
        // given
        val photo = bodyPhoto(reviewCount = 10, createdAt = since.minus(1, ChronoUnit.MICROS))

        // when
        val inPool = photo.isInRankingPool(since)

        // then
        assertFalse(inPool)
    }

    @Test
    @DisplayName("생성 시각이 기준 시각과 같아도 리뷰가 완료되지 않았으면 순위 집계 대상이 아니다")
    fun incompleteAtSinceIsNotInPool() {
        // given
        val photo = bodyPhoto(reviewCount = 9, createdAt = since)

        // when
        val inPool = photo.isInRankingPool(since)

        // then
        assertFalse(inPool)
    }

    private fun bodyPhoto(reviewCount: Int, createdAt: LocalDateTime) = BodyPhoto(
        bodyPhotoId = 1L,
        memberId = 1L,
        imageUrl = "https://bucket/photo.jpg",
        reviewCount = reviewCount,
        totalReviewScore = reviewCount * 4.0,
        createdAt = createdAt,
        updatedAt = null,
    )

}
