package app.thunder.api.domain.photo

import java.time.LocalDateTime
import kotlin.math.round

class BodyPhoto private constructor(
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

    companion object {
        fun from(entity: BodyPhotoEntity): BodyPhoto {
            return BodyPhoto(
                entity.bodyPhotoId,
                entity.memberId,
                entity.imageUrl,
                entity.reviewCount,
                entity.totalReviewScore,
                entity.createdAt,
                entity.updatedAt,
            )
        }

        private const val REVIEW_COMPLETE_COUNT: Int = 20
        private const val RESULT_MAX_SCORE: Double = 10.0
        private const val REVIEW_MAX_SCORE: Double = 5.0
    }

    fun isReviewCompleted(): Boolean {
        val before1Day = LocalDateTime.now().minusDays(1)
        return this.reviewCount >= REVIEW_COMPLETE_COUNT || this.createdAt.isBefore(before1Day)
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

}