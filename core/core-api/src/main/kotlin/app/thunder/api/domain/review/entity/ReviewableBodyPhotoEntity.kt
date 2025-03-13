package app.thunder.api.domain.review.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import java.time.LocalDateTime

@Table(name = "reviewable_body_photo")
@Entity
@IdClass(ReviewableBodyPhotoId::class) // TODO: CHANGE IDENTITY PK
class ReviewableBodyPhotoEntity private constructor(
    memberId: Long,
    bodyPhotoId: Long,
    bodyPhotoMemberId: Long,
    createdAt: LocalDateTime,
) {
    @Id
    @Column(name = "member_id", nullable = false)
    val memberId: Long = memberId

    @Column(name = "body_photo_id", nullable = false)
    val bodyPhotoId: Long = bodyPhotoId

    @Column(name = "body_photo_member_id", nullable = false)
    val bodyPhotoMemberId: Long = bodyPhotoMemberId

    @Id
    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = createdAt


    companion object {
        fun create(memberId: Long,
                   bodyPhotoId: Long,
                   bodyPhotoMemberId: Long,
                   createdAt: LocalDateTime): ReviewableBodyPhotoEntity {
            return ReviewableBodyPhotoEntity(memberId, bodyPhotoId, bodyPhotoMemberId, createdAt)
        }
    }

}