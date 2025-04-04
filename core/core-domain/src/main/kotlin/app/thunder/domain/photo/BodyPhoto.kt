package app.thunder.domain.photo

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

    fun isNotUploader(memberId: Long): Boolean {
        return this.memberId != memberId
    }

    fun progressRate(): Double {
        return (this.reviewCount / 20.0 * 100).coerceAtMost(100.0)
    }

    fun addReview(score: Int) {
        this.reviewCount += 1
        this.totalReviewScore += score
        this.updatedAt = LocalDateTime.now()
    }

    fun getResultReviewScore(): Double {
        if (this.reviewCount <= 0) {
            return 0.0
        }
        val averageScore = this.totalReviewScore / this.reviewCount
        val resultScore = averageScore * RESULT_MAX_SCORE / REVIEW_MAX_SCORE
        return round(resultScore * 10) / 10
    }

    companion object {
        const val REVIEW_COMPLETE_COUNT: Int = 10
        private const val RESULT_MAX_SCORE: Double = 10.0
        private const val REVIEW_MAX_SCORE: Double = 5.0
    }

}