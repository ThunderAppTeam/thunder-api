package app.thunder.infrastructure.db.review.entity

import jakarta.persistence.Column
import jakarta.persistence.Id
import java.io.Serializable
import java.time.LocalDateTime

internal data class ReviewableBodyPhotoId(
    @Id
    @Column(name = "member_id", nullable = false)
    val memberId: Long = 0,

    @Id
    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
) : Serializable