package app.thunder.api.domain.photo

import app.thunder.api.domain.member.Gender
import app.thunder.api.exception.BodyErrors
import app.thunder.api.exception.ThunderException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class BodyPhotoAdapter(
    val bodyPhotoRepository: BodyPhotoRepository,
) {

    @Transactional(readOnly = true)
    fun getById(bodyPhotoId: Long): BodyPhoto {
        val entity = bodyPhotoRepository.findById(bodyPhotoId)
            .orElseThrow { ThunderException(BodyErrors.NOT_FOUND_BODY_PHOTO) }
        return BodyPhoto.from(entity)
    }

    @Transactional(readOnly = true)
    fun getAllNotReviewCompleted(): List<BodyPhoto> {
        return bodyPhotoRepository.findAllNotReviewCompleted()
            .map(BodyPhoto::from)
    }

    @Transactional(readOnly = true)
    fun getAllByMemberId(memberId: Long): List<BodyPhoto> {
        return bodyPhotoRepository.findAllByMemberId(memberId)
            .map(BodyPhoto::from)
    }

    @Transactional(readOnly = true)
    fun getAllById(bodyPhotoIds: Collection<Long>): List<BodyPhoto> {
        return bodyPhotoRepository.findAllById(bodyPhotoIds)
            .map(BodyPhoto::from)
    }

    @Transactional(readOnly = true)
    fun getAllByGender(gender: Gender): List<BodyPhoto> {
        return bodyPhotoRepository.findAllByGender(gender)
            .map(BodyPhoto::from)
    }

    @Transactional
    fun create(memberId: Long, imageUrl: String): BodyPhoto {
        val entity = BodyPhotoEntity.create(memberId, imageUrl)
        bodyPhotoRepository.save(entity)
        return BodyPhoto.from(entity)
    }

    @Transactional
    fun update(bodyPhoto: BodyPhoto) {
        val bodyPhotoEntity = bodyPhotoRepository.findById(bodyPhoto.bodyPhotoId)
            .orElseThrow { ThunderException(BodyErrors.NOT_FOUND_BODY_PHOTO) }
        bodyPhotoEntity.update(reviewCount = bodyPhoto.reviewCount,
                               totalReviewScore = bodyPhoto.totalReviewScore,
                               updatedAt = bodyPhoto.updatedAt)
    }

    @Transactional
    fun deleteById(bodyPhotoId: Long) {
        bodyPhotoRepository.deleteById(bodyPhotoId)
    }

    @Transactional
    fun deleteAllByMemberId(memberId: Long) {
        val ids = bodyPhotoRepository.findAllByMemberId(memberId)
            .map { it.bodyPhotoId }
        bodyPhotoRepository.deleteAllByIdInBatch(ids)
    }

}