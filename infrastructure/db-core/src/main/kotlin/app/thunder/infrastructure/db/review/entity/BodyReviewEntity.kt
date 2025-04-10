package app.thunder.infrastructure.db.review.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Table(name = "body_review")
@Entity
internal class BodyReviewEntity private constructor(
    bodyPhotoId: Long,
    memberId: Long,
    score: Int,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "body_review_id")
    val bodyReviewId: Long = 0

    @Column(name = "body_photo_id")
    val bodyPhotoId: Long = bodyPhotoId

    @Column(name = "member_id")
    val memberId: Long = memberId

    @Column(name = "score")
    val score: Int = score

    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now()


    companion object {
        internal fun create(
            bodyPhotoId: Long,
            memberId: Long,
            score: Int,
        ): BodyReviewEntity {
            return BodyReviewEntity(
                bodyPhotoId = bodyPhotoId,
                memberId = memberId,
                score = score,
            )
        }
    }

}