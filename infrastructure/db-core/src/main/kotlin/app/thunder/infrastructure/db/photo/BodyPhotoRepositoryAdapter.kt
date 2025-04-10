package app.thunder.infrastructure.db.photo

import app.thunder.domain.member.Gender
import app.thunder.domain.photo.BodyPhoto
import app.thunder.domain.photo.BodyPhotoPort
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
internal class BodyPhotoRepositoryAdapter(
    val bodyPhotoJpaRepository: BodyPhotoJpaRepository,
) : BodyPhotoPort {

    @Transactional(readOnly = true)
    override fun getById(bodyPhotoId: Long): BodyPhoto? {
        val entity = bodyPhotoJpaRepository.findById(bodyPhotoId)
            .orElse(null)
            ?: return null
        return entityToDomain(entity)
    }

    @Transactional(readOnly = true)
    override fun getAllById(bodyPhotoIds: Collection<Long>): List<BodyPhoto> {
        return bodyPhotoJpaRepository.findAllById(bodyPhotoIds)
            .map(::entityToDomain)
    }

    @Transactional(readOnly = true)
    override fun getAllByMemberId(memberId: Long): List<BodyPhoto> {
        return bodyPhotoJpaRepository.findAllByMemberId(memberId)
            .map(::entityToDomain)
    }

    @Transactional(readOnly = true)
    override fun getAllByGender(gender: Gender): List<BodyPhoto> {
        return bodyPhotoJpaRepository.findAllByGender(gender)
            .map(::entityToDomain)
    }

    @Transactional(readOnly = true)
    override fun getNotReviewCompletedAll(): List<BodyPhoto> {
        return bodyPhotoJpaRepository.findAllNotReviewCompleted()
            .map(::entityToDomain)
    }

    @Transactional
    override fun create(memberId: Long, imageUrl: String): BodyPhoto {
        val entity = BodyPhotoEntity.create(memberId, imageUrl)
        bodyPhotoJpaRepository.save(entity)
        return entityToDomain(entity)
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


    private fun entityToDomain(entity: BodyPhotoEntity): BodyPhoto {
        return BodyPhoto(
            entity.bodyPhotoId,
            entity.memberId,
            entity.imageUrl,
            entity.reviewCount,
            entity.totalReviewScore,
            entity.createdAt,
            entity.updatedAt,
        )
    }

}
