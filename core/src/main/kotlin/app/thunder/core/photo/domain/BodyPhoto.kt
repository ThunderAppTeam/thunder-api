package app.thunder.core.photo.domain

import java.time.LocalDateTime
import kotlin.math.round

class BodyPhoto(
    val bodyPhotoId: Long,
    val memberId: Long,
    val imageUrl: String,
    reviewCount: Int,
    totalReviewScore: Double,
    val createdAt: LocalDateTime,
    updatedAt: LocalDateTime?,
) {
    var reviewCount: Int = reviewCount
        private set
    var totalReviewScore: Double = totalReviewScore
        private set
    var updatedAt: LocalDateTime? = updatedAt
        private set

    fun isReviewCompleted(): Boolean {
        return this.reviewCount >= REVIEW_COMPLETE_COUNT
    }

    fun isInRankingPool(since: LocalDateTime): Boolean {
        return isReviewCompleted() && !this.createdAt.isBefore(since)
    }

    fun isUploader(memberId: Long): Boolean {
        return this.memberId == memberId
    }

    fun isNotUploader(memberId: Long): Boolean {
        return !isUploader(memberId)
    }

    fun progressRate(): Double {
        return (this.reviewCount.toDouble() / REVIEW_COMPLETE_COUNT * 100).coerceAtMost(100.0)
    }

    fun averageScore(): Double {
        if (this.reviewCount <= 0) {
            return 0.0
        }
        return this.totalReviewScore / this.reviewCount
    }

    fun getResultReviewScore(): Double {
        if (this.reviewCount <= 0) {
            return 0.0
        }
        val resultScore = averageScore() * RESULT_MAX_SCORE / REVIEW_MAX_SCORE
        return round(resultScore * 10) / 10
    }

    companion object {
        const val REVIEW_COMPLETE_COUNT: Int = 10
        const val RANKING_PERIOD_DAYS: Long = 30L
        private const val RESULT_MAX_SCORE: Double = 10.0
        private const val REVIEW_MAX_SCORE: Double = 5.0
    }

}