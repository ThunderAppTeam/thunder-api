package app.thunder.api.domain.body

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class BodyReviewAdapter(
    val bodyReviewRepository: BodyReviewRepository
) {

    @Transactional
    fun create(bodyPhotoId: Long, memberId: Long, score: Int): BodyReview {
        val entity = BodyReviewEntity.create(bodyPhotoId, memberId, score)
        bodyReviewRepository.save(entity)
        return BodyReview.from(entity)
    }

    fun getCountByBodyPhotoId(bodyPhotoId: Long): Long {
        return bodyReviewRepository.countByBodyPhotoId(bodyPhotoId)
    }

}