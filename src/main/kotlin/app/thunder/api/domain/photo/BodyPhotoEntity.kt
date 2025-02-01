package app.thunder.api.domain.photo

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Table(name = "body_photo")
@Entity
class BodyPhotoEntity private constructor(
    memberId: Long,
    imageUrl: String,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "body_photo_id")
    val bodyPhotoId: Long = 0

    @Column(name = "member_id")
    val memberId: Long = memberId

    @Column(name = "image_url")
    val imageUrl: String = imageUrl

    @Column(name = "review_count")
    var reviewCount: Int = 0
        protected set

    @Column(name = "review_score")
    var reviewScore: Double = 0.0
        protected set

    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now()

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null
        protected set


    companion object {
        fun create(
            memberId: Long,
            imageUrl: String,
        ): BodyPhotoEntity {
            return BodyPhotoEntity(
                memberId = memberId,
                imageUrl = imageUrl,
            )
        }
    }

    fun update(reviewCount: Int, reviewScore: Double, updatedAt: LocalDateTime?) {
        this.reviewCount = reviewCount
        this.reviewScore = reviewScore
        this.updatedAt = updatedAt
    }

}