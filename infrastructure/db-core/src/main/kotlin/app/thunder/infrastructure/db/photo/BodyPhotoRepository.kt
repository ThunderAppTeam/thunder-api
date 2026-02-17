package app.thunder.infrastructure.db.photo

import app.thunder.domain.member.Gender
import app.thunder.domain.photo.BodyPhoto
import app.thunder.domain.photo.BodyPhotoPort
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
internal class BodyPhotoRepository(
    val bodyPhotoJpaRespository: BodyPhotoJpaRespository,
) : BodyPhotoPort {

    @Transactional(readOnly = true)
    override fun getById(bodyPhotoId: Long): BodyPhoto? {
        val entity = bodyPhotoJpaRespository.findById(bodyPhotoId)
            .orElse(null)
            ?: return null
        return entityToDomain(entity)
    }

    @Transactional(readOnly = true)
    override fun getAllById(bodyPhotoIds: Collection<Long>): List<BodyPhoto> {
        return bodyPhotoJpaRespository.findAllById(bodyPhotoIds)
            .map(::entityToDomain)
    }

    @Transactional(readOnly = true)
    override fun getAllByMemberId(memberId: Long): List<BodyPhoto> {
        return bodyPhotoJpaRespository.findAllByMemberId(memberId)
            .map(::entityToDomain)
    }

    @Transactional(readOnly = true)
    override fun getAllByGender(gender: Gender): List<BodyPhoto> {
        return bodyPhotoJpaRespository.findAllByGender(gender)
            .map(::entityToDomain)
    }

    @Transactional(readOnly = true)
    override fun getNotReviewCompletedAll(): List<BodyPhoto> {
        return bodyPhotoJpaRespository.findAllNotReviewCompleted()
            .map(::entityToDomain)
    }

    @Transactional
    override fun create(memberId: Long, imageUrl: String): BodyPhoto {
        val entity = BodyPhotoEntity.create(memberId, imageUrl)
        bodyPhotoJpaRespository.save(entity)
        return entityToDomain(entity)
    }

    @Transactional
    override fun update(bodyPhoto: BodyPhoto) {
        bodyPhotoJpaRespository.findById(bodyPhoto.bodyPhotoId)
            .ifPresent { bodyPhotoEntity ->
                bodyPhotoEntity.update(reviewCount = bodyPhoto.reviewCount,
                                       totalReviewScore = bodyPhoto.totalReviewScore,
                                       updatedAt = bodyPhoto.updatedAt)
            }
    }

    @Transactional
    override fun deleteById(bodyPhotoId: Long) {
        bodyPhotoJpaRespository.deleteById(bodyPhotoId)
    }

    @Transactional
    override fun deleteAllByMemberId(memberId: Long) {
        val ids = bodyPhotoJpaRespository.findAllByMemberId(memberId)
            .map { it.bodyPhotoId }
        bodyPhotoJpaRespository.deleteAllByIdInBatch(ids)
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
