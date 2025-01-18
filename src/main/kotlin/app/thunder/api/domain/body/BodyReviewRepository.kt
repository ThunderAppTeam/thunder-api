package app.thunder.api.domain.body

import org.springframework.data.jpa.repository.JpaRepository

interface BodyReviewRepository : JpaRepository<BodyReviewEntity, Long> {
    fun existsByBodyPhotoIdAndMemberId(bodyPhotoId: Long, memberId: Long): Boolean
    fun countByBodyPhotoId(bodyPhotoId: Long): Long
}
