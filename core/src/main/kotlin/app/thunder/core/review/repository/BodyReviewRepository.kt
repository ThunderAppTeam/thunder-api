package app.thunder.core.review.repository

import app.thunder.core.review.domain.BodyReview

interface BodyReviewRepository {
    fun getAllByMemberId(memberId: Long): List<BodyReview>
    fun getMemberIdToLatestReviewMap(): Map<Long, BodyReview>
    fun existsByBodyPhotoIdAndMemberId(bodyPhotoId: Long, memberId: Long): Boolean
    fun create(bodyPhotoId: Long, memberId: Long, score: Int): BodyReview
}
