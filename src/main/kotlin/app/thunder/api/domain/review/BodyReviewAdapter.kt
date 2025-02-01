package app.thunder.api.domain.review

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class BodyReviewAdapter(
    val bodyReviewRepository: BodyReviewRepository
) {

    @Transactional(readOnly = true)
    fun getAllByBodyPhotoId(bodyPhotoId: Long): List<BodyReview> {
        return bodyReviewRepository.findAllByBodyPhotoIdIn(listOf(bodyPhotoId))
            .map(BodyReview::from)
    }

    @Transactional(readOnly = true)
    fun getAllByMemberId(memberId: Long): List<BodyReview> {
        return bodyReviewRepository.findAllByMemberId(memberId)
            .map(BodyReview::from)
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