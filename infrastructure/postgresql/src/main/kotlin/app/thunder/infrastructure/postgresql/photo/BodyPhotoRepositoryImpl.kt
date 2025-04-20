package app.thunder.infrastructure.postgresql.photo

import app.thunder.core.member.domain.Gender
import app.thunder.core.photo.domain.BodyPhoto
import app.thunder.core.photo.repository.BodyPhotoRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
internal class BodyPhotoRepositoryImpl(
    val bodyPhotoJpaRepository: BodyPhotoJpaRepository,
) : BodyPhotoRepository {

    @Transactional(readOnly = true)
    override fun getById(bodyPhotoId: Long): BodyPhoto? {
        val entity = bodyPhotoJpaRepository.findById(bodyPhotoId)
            .orElse(null)
            ?: return null
        return entity.toDomain()
    }

    @Transactional(readOnly = true)
    override fun getAllById(bodyPhotoIds: Collection<Long>): List<BodyPhoto> {
        return bodyPhotoJpaRepository.findAllById(bodyPhotoIds)
            .map(BodyPhotoEntity::toDomain)
    }

    @Transactional(readOnly = true)
    override fun getAllByMemberId(memberId: Long): List<BodyPhoto> {
        return bodyPhotoJpaRepository.findAllByMemberId(memberId)
            .map(BodyPhotoEntity::toDomain)
    }

    @Transactional(readOnly = true)
    override fun getAllByGender(gender: Gender): List<BodyPhoto> {
        return bodyPhotoJpaRepository.findAllByGender(gender)
            .map(BodyPhotoEntity::toDomain)
    }

    @Transactional(readOnly = true)
    override fun countByGender(gender: Gender): Long {
        return bodyPhotoJpaRepository.countByGender(gender)
    }

    @Transactional(readOnly = true)
    override fun countByGenderWithHigherScore(gender: Gender, score: Double): Long {
        return bodyPhotoJpaRepository.countByGenderWithHigherScore(gender, score)
    }

    @Transactional(readOnly = true)
    override fun getNotReviewCompletedAll(): List<BodyPhoto> {
        return bodyPhotoJpaRepository.findAllNotReviewCompleted()
            .map(BodyPhotoEntity::toDomain)
    }

    @Transactional
    override fun create(memberId: Long, imageUrl: String): BodyPhoto {
        val entity = BodyPhotoEntity.create(memberId, imageUrl)
        bodyPhotoJpaRepository.save(entity)
        return entity.toDomain()
    }

    @Transactional
    override fun update(bodyPhoto: BodyPhoto) {
        bodyPhotoJpaRepository.findById(bodyPhoto.bodyPhotoId)
            .ifPresent { bodyPhotoEntity ->
                bodyPhotoEntity.update(reviewCount = bodyPhoto.reviewCount,
                                       totalReviewScore = bodyPhoto.totalReviewScore,
                                       updatedAt = bodyPhoto.updatedAt)
            }
    }

    @Transactional
    override fun deleteById(bodyPhotoId: Long) {
        bodyPhotoJpaRepository.deleteById(bodyPhotoId)
    }

    @Transactional
    override fun deleteAllByMemberId(memberId: Long) {
        val ids = bodyPhotoJpaRepository.findAllByMemberId(memberId)
            .map { it.bodyPhotoId }
        bodyPhotoJpaRepository.deleteAllByIdInBatch(ids)
    }

}
