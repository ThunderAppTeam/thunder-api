package app.thunder.api.domain.body

import jakarta.persistence.*
import java.time.LocalDateTime

@Table(name = "body_review")
@Entity
class BodyReviewEntity private constructor(
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
        fun create(
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