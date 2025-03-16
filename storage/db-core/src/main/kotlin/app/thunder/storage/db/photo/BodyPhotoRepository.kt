package app.thunder.storage.db.photo

import app.thunder.domain.member.Gender
import app.thunder.domain.photo.BodyPhoto
import app.thunder.domain.photo.BodyPhotoAdapter
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
internal class BodyPhotoRepository(
    val bodyPhotoPersistence: BodyPhotoPersistence,
) : BodyPhotoAdapter {

    @Transactional(readOnly = true)
    override fun getById(bodyPhotoId: Long): BodyPhoto? {
        val entity = bodyPhotoPersistence.findById(bodyPhotoId)
            .orElse(null)
            ?: return null
        return entityToDomain(entity)
    }

    @Transactional(readOnly = true)
    override fun getAllById(bodyPhotoIds: Collection<Long>): List<BodyPhoto> {
        return bodyPhotoPersistence.findAllById(bodyPhotoIds)
            .map(::entityToDomain)
    }

    @Transactional(readOnly = true)
    override fun getAllByMemberId(memberId: Long): List<BodyPhoto> {
        return bodyPhotoPersistence.findAllByMemberId(memberId)
            .map(::entityToDomain)
    }

    @Transactional(readOnly = true)
    override fun getAllByGender(gender: Gender): List<BodyPhoto> {
        return bodyPhotoPersistence.findAllByGender(gender)
            .map(::entityToDomain)
    }

    @Transactional(readOnly = true)
    override fun getNotReviewCompletedAll(): List<BodyPhoto> {
        return bodyPhotoPersistence.findAllNotReviewCompleted()
            .map(::entityToDomain)
    }

    @Transactional
    override fun create(memberId: Long, imageUrl: String): BodyPhoto {
        val entity = BodyPhotoEntity.create(memberId, imageUrl)
        bodyPhotoPersistence.save(entity)
        return entityToDomain(entity)
    }

    @Transactional
    override fun update(bodyPhoto: BodyPhoto) {
        bodyPhotoPersistence.findById(bodyPhoto.bodyPhotoId)
            .ifPresent { bodyPhotoEntity ->
                bodyPhotoEntity.update(reviewCount = bodyPhoto.reviewCount,
                                       totalReviewScore = bodyPhoto.totalReviewScore,
                                       updatedAt = bodyPhoto.updatedAt)
            }
    }

    @Transactional
    override fun deleteById(bodyPhotoId: Long) {
        bodyPhotoPersistence.deleteById(bodyPhotoId)
    }

    @Transactional
    override fun deleteAllByMemberId(memberId: Long) {
        val ids = bodyPhotoPersistence.findAllByMemberId(memberId)
            .map { it.bodyPhotoId }
        bodyPhotoPersistence.deleteAllByIdInBatch(ids)
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