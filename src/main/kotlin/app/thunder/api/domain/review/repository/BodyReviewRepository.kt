package app.thunder.api.domain.review.repository

import app.thunder.api.domain.review.entity.BodyReviewEntity
import org.springframework.data.jpa.repository.JpaRepository

interface BodyReviewRepository : JpaRepository<BodyReviewEntity, Long> {
    fun existsByBodyPhotoIdAndMemberId(bodyPhotoId: Long, memberId: Long): Boolean
    fun findAllByMemberId(memberId: Long): List<BodyReviewEntity>
}
