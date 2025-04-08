package app.thunder.storage.db.review

import app.thunder.domain.review.ReviewableBodyPhoto
import app.thunder.domain.review.ReviewableBodyPhotoPort
import app.thunder.domain.review.command.CreateReviewableBodyPhotoCommand
import app.thunder.storage.db.review.entity.ReviewableBodyPhotoEntity
import app.thunder.storage.db.review.entity.ReviewableBodyPhotoId
import app.thunder.storage.db.review.jdbc.ReviewableBodyPhotoJdbcRepository
import app.thunder.storage.db.review.persistence.ReviewableBodyPhotoJpaRepository
import app.thunder.storage.db.review.persistence.findAllByMemberId
import app.thunder.storage.db.review.persistence.findFirstAllByMemberIds
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
internal class ReviewableBodyPhotoRepository(
    private val reviewableBodyPhotoJpaRepository: ReviewableBodyPhotoJpaRepository,
    private val reviewableBodyPhotoJdbcRepository: ReviewableBodyPhotoJdbcRepository,
) : ReviewableBodyPhotoPort {

    @Transactional(readOnly = true)
    override fun getAllByMemberId(memberId: Long, limit: Int?): List<ReviewableBodyPhoto> {
        return reviewableBodyPhotoJpaRepository.findAllByMemberId(memberId, limit)
            .map(::entityToDomain)
    }

    @Transactional(readOnly = true)
    override fun getAllByBodyPhotoId(bodyPhotoId: Long): List<ReviewableBodyPhoto> {
        return reviewableBodyPhotoJpaRepository.findAllByBodyPhotoId(bodyPhotoId)
            .map(::entityToDomain)
    }

    @Transactional(readOnly = true)
    override fun getFirstByMemberIds(memberIds: Collection<Long>): List<ReviewableBodyPhoto> {
        return reviewableBodyPhotoJpaRepository.findFirstAllByMemberIds(memberIds)
            .map(::entityToDomain)
    }

    @Transactional
    override fun deleteByMemberIdAndBodyPhotoId(memberId: Long, bodyPhotoId: Long) {
        reviewableBodyPhotoJpaRepository.deleteByMemberIdAndBodyPhotoId(memberId, bodyPhotoId)
    }

    @Transactional
    override fun deleteAllByMemberId(memberId: Long) {
        val ids = hashSetOf<ReviewableBodyPhotoId>()
        reviewableBodyPhotoJpaRepository.findAllByMemberId(memberId)
            .forEach { ids.add(ReviewableBodyPhotoId(it.memberId, it.createdAt)) }
        reviewableBodyPhotoJpaRepository.findAllByBodyPhotoMemberId(memberId)
            .forEach { ids.add(ReviewableBodyPhotoId(it.memberId, it.createdAt)) }
        reviewableBodyPhotoJpaRepository.deleteAllByIdInBatch(ids)
    }

    @Transactional
    override fun deleteAllByBodyPhotoId(bodyPhotoId: Long) {
        val ids = reviewableBodyPhotoJpaRepository.findAllByBodyPhotoId(bodyPhotoId)
            .map { ReviewableBodyPhotoId(it.memberId, it.createdAt) }
        reviewableBodyPhotoJpaRepository.deleteAllByIdInBatch(ids)
    }

    @Transactional
    override fun deleteAllByMemberIdAndPhotoMemberId(memberId: Long, bodyPhotoMemberId: Long) {
        val reviewableBodyPhotoIds = reviewableBodyPhotoJpaRepository.findAllByMemberId(memberId)
            .asSequence()
            .filter { it.bodyPhotoMemberId == bodyPhotoMemberId }
            .map { ReviewableBodyPhotoId(it.memberId, it.createdAt) }
            .toList()
        reviewableBodyPhotoJpaRepository.deleteAllByIdInBatch(reviewableBodyPhotoIds)
    }

    @Transactional
    override fun saveAll(commands: Collection<CreateReviewableBodyPhotoCommand>) {
        val entities = commands.map { command ->
            ReviewableBodyPhotoEntity.create(
                command.bodyPhotoId,
                command.memberId,
                command.bodyPhotoMemberId
            )
        }
        reviewableBodyPhotoJdbcRepository.batchInsert(entities)
    }


    private fun entityToDomain(entity: ReviewableBodyPhotoEntity): ReviewableBodyPhoto {
        return ReviewableBodyPhoto(
            entity.memberId,
            entity.bodyPhotoId,
            entity.bodyPhotoMemberId,
            entity.createdAt,
        )
    }

}
