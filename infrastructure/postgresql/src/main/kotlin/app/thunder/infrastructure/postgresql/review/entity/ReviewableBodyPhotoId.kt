package app.thunder.infrastructure.postgresql.review.entity

import jakarta.persistence.Column
import jakarta.persistence.Id
import java.io.Serializable

internal data class ReviewableBodyPhotoId(
    @Id
    @Column(name = "member_id", nullable = false)
    val memberId: Long = 0,

    @Id
    @Column(name = "body_photo_id", nullable = false)
    val bodyPhotoId: Long = 0,
) : Serializable