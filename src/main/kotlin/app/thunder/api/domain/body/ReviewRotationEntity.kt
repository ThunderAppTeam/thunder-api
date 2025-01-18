package app.thunder.api.domain.body

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes

@Table(name = "review_rotation_queue")
@Entity
class ReviewRotationEntity private constructor(
    bodyPhotoId: Long,
    reviewMemberIds: Set<Long>
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_rotation_id", unique = true)
    val reviewRotationId: Long = 0

    @Column(name = "body_photo_id")
    val bodyPhotoId: Long = bodyPhotoId

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "reviewed_member_ids", columnDefinition = "JSONB")
    var reviewedMemberIds: Set<Long> = reviewMemberIds
        protected set

    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now()


    companion object {
        fun create(bodyPhotoId: Long): ReviewRotationEntity {
            return ReviewRotationEntity(bodyPhotoId, emptySet())
        }

        fun copy(source: ReviewRotationEntity): ReviewRotationEntity {
            return ReviewRotationEntity(source.bodyPhotoId, source.reviewedMemberIds)
        }
    }

    fun update(reviewedMemberIds: Set<Long>) {
        this.reviewedMemberIds = reviewedMemberIds
    }

}