package app.thunder.api.domain.body

import jakarta.persistence.EntityManager
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class ReviewRotationAdapter(
    val reviewRotationQueueRepository: ReviewRotationQueueRepository,
    val entityManager: EntityManager,
) {

    @Transactional(readOnly = true)
    fun getAllByIdGteAndMemberIdNot(reviewRotationId: Long, memberId: Long, fetchSize: Int): List<ReviewRotation> {
        return reviewRotationQueueRepository.getAllByIdGteAndMemberIdNot(reviewRotationId, memberId, fetchSize)
            .map { ReviewRotation.from(it) }
    }

    @Transactional
    fun create(bodyPhotoId: Long, memberId: Long): ReviewRotation {
        val entity = ReviewRotationEntity.create(bodyPhotoId, memberId)
        reviewRotationQueueRepository.save(entity)
        return ReviewRotation.from(entity)
    }

    @Transactional
    fun refresh(bodyPhotoIds: Collection<Long>, memberId: Long) {
        val entities = reviewRotationQueueRepository.findAllByBodyPhotoIdIn(bodyPhotoIds)
        val copies = entities.map {
            ReviewRotationEntity.create(
                bodyPhotoId = it.bodyPhotoId,
                memberId = it.memberId,
                reviewedMemberIds = it.reviewedMemberIds + memberId
            )
        }
        reviewRotationQueueRepository.deleteAll(entities)
        entityManager.flush()
        reviewRotationQueueRepository.saveAll(copies)
    }

    @Transactional
    fun deleteByBodyPhotoId(bodyPhotoId: Long) {
        reviewRotationQueueRepository.deleteByBodyPhotoId(bodyPhotoId)
    }

}