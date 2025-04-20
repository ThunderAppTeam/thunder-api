package app.thunder.infrastructure.postgresql.review

import app.thunder.core.review.domain.BodyReview
import app.thunder.core.review.repository.BodyReviewRepository
import app.thunder.infrastructure.postgresql.review.entity.BodyReviewEntity
import app.thunder.infrastructure.postgresql.review.persistence.BodyReviewJpaRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
internal class BodyReviewRepositoryImpl(
    private val bodyReviewJpaRepository: BodyReviewJpaRepository
) : BodyReviewRepository {

    @Transactional(readOnly = true)
    override fun getAllByMemberId(memberId: Long): List<BodyReview> {
        return bodyReviewJpaRepository.findAllByMemberId(memberId)
            .map(::entityToDomain)
    }

    @Transactional(readOnly = true)
    override fun getMemberIdToLatestReviewMap(): Map<Long, BodyReview> {
        return bodyReviewJpaRepository.findLatestGroupByMemberId()
            .map(::entityToDomain)
            .associateBy { it.memberId }
    }

    @Transactional(readOnly = true)
    override fun existsByBodyPhotoIdAndMemberId(bodyPhotoId: Long, memberId: Long): Boolean {
        return bodyReviewJpaRepository.existsByBodyPhotoIdAndMemberId(bodyPhotoId, memberId)
    }

    @Transactional
    override fun create(bodyPhotoId: Long, memberId: Long, score: Int): BodyReview {
        val entity = BodyReviewEntity.create(bodyPhotoId, memberId, score)
        bodyReviewJpaRepository.save(entity)
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
