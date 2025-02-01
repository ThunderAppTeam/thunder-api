package app.thunder.api.domain.review

import org.springframework.data.jpa.repository.JpaRepository

interface BodyReviewRepository : JpaRepository<BodyReviewEntity, Long> {
    fun existsByBodyPhotoIdAndMemberId(bodyPhotoId: Long, memberId: Long): Boolean
    fun findAllByMemberId(memberId: Long): List<BodyReviewEntity>
    fun findAllByBodyPhotoIdIn(bodyPhotoIds: Collection<Long>): List<BodyReviewEntity>
}
