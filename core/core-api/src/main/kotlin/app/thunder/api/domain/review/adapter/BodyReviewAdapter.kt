package app.thunder.api.domain.review.adapter

import app.thunder.api.domain.review.BodyReview
import app.thunder.api.domain.review.entity.BodyReviewEntity
import app.thunder.api.domain.review.repository.BodyReviewRepository
import app.thunder.api.domain.review.repository.findLatestGroupByMemberId
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class BodyReviewAdapter(
    val bodyReviewRepository: BodyReviewRepository
) {

    @Transactional(readOnly = true)
    fun getMemberIdToLatestReviewMap(): Map<Long, BodyReview> {
        return bodyReviewRepository.findLatestGroupByMemberId()
            .map(BodyReview::from)
            .associateBy { it.memberId }
    }

    @Transactional(readOnly = true)
    fun existsByBodyPhotoIdAndMemberId(bodyPhotoId: Long, memberId: Long): Boolean {
        return bodyReviewRepository.existsByBodyPhotoIdAndMemberId(bodyPhotoId, memberId)
    }

    @Transactional
    fun create(bodyPhotoId: Long, memberId: Long, score: Int): BodyReview {
        val entity = BodyReviewEntity.create(bodyPhotoId, memberId, score)
        bodyReviewRepository.save(entity)
        return BodyReview.from(entity)
    }

}