package app.thunder.storage.db.review

import app.thunder.domain.review.BodyReview
import app.thunder.domain.review.BodyReviewAdapter
import app.thunder.storage.db.review.entity.BodyReviewEntity
import app.thunder.storage.db.review.persistence.BodyReviewPersistence
import app.thunder.storage.db.review.persistence.findLatestGroupByMemberId
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
internal class BodyReviewRepository(
    private val bodyReviewPersistence: BodyReviewPersistence
) : BodyReviewAdapter {

    @Transactional(readOnly = true)
    override fun getAllByMemberId(memberId: Long): List<BodyReview> {
        return bodyReviewPersistence.findAllByMemberId(memberId)
            .map(::entityToDomain)
    }

    @Transactional(readOnly = true)
    override fun getMemberIdToLatestReviewMap(): Map<Long, BodyReview> {
        return bodyReviewPersistence.findLatestGroupByMemberId()
            .map(::entityToDomain)
            .associateBy { it.memberId }
    }

    @Transactional(readOnly = true)
    override fun existsByBodyPhotoIdAndMemberId(bodyPhotoId: Long, memberId: Long): Boolean {
        return bodyReviewPersistence.existsByBodyPhotoIdAndMemberId(bodyPhotoId, memberId)
    }

    @Transactional
    override fun create(bodyPhotoId: Long, memberId: Long, score: Int): BodyReview {
        val entity = BodyReviewEntity.create(bodyPhotoId, memberId, score)
        bodyReviewPersistence.save(entity)
        return entityToDomain(entity)
    }


    private fun entityToDomain(entity: BodyReviewEntity): BodyReview {
        return BodyReview(
            entity.bodyReviewId,
            entity.bodyPhotoId,
            entity.memberId,
            entity.score,
            entity.createdAt
        )
    }

}