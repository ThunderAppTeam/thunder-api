package app.thunder.api.domain.body

import app.thunder.api.exception.BodyErrors.NOT_FOUND_REVIEW_ROTATION
import app.thunder.api.exception.ThunderException
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class ReviewRotationAdapter(
    val reviewRotationQueueRepository: ReviewRotationQueueRepository,
    val entityManager: EntityManager,
) {

    @Transactional(readOnly = true)
    fun getByBodyPhotoId(bodyPhotoId: Long): ReviewRotation {
        val entity = reviewRotationQueueRepository.findByBodyPhotoId(bodyPhotoId)
            .orElseThrow { ThunderException(NOT_FOUND_REVIEW_ROTATION) }
        return ReviewRotation.from(entity)
    }

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
    fun refresh(bodyPhotoIds: Collection<Long>) {
        val entities = reviewRotationQueueRepository.findAllByBodyPhotoIdIn(bodyPhotoIds)
        val copies = entities.map { ReviewRotationEntity.copy(it) }
        reviewRotationQueueRepository.deleteAll(entities)
        entityManager.flush()
        reviewRotationQueueRepository.saveAll(copies)
    }

    @Transactional
    fun update(reviewRotation: ReviewRotation) {
        val entity = reviewRotationQueueRepository.findById(reviewRotation.reviewRotationId)
            .orElseThrow { ThunderException(NOT_FOUND_REVIEW_ROTATION) }
        entity.update(reviewRotation.reviewedMemberIds)
    }

}