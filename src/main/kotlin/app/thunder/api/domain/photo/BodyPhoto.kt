package app.thunder.api.domain.photo

import java.time.LocalDateTime
import kotlin.math.round

class BodyPhoto private constructor(
    val bodyPhotoId: Long,
    val memberId: Long,
    val imageUrl: String,
    reviewCount: Int,
    reviewScore: Double,
    val createdAt: LocalDateTime,
    updatedAt: LocalDateTime?,
) {
    var reviewCount: Int = reviewCount
        private set
    var reviewScore: Double = reviewScore
        get() = round(field * 10) / 10
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
                entity.reviewScore,
                entity.createdAt,
                entity.updatedAt,
            )
        }

        private const val REVIEW_COMPLETE_COUNT: Int = 20
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
        val totalScore = (this.reviewScore * this.reviewCount) + (score * 2)
        this.reviewCount += 1
        this.reviewScore = (totalScore / (this.reviewCount)) * 2
        this.updatedAt = LocalDateTime.now()
    }

}