package app.thunder.api.domain.body

import app.thunder.api.exception.BodyErrors
import app.thunder.api.exception.ThunderException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class BodyPhotoAdapter(
    val bodyPhotoRepository: BodyPhotoRepository
) {

    fun getById(bodyPhotoId: Long): BodyPhoto {
        val entity = bodyPhotoRepository.findById(bodyPhotoId)
            .orElseThrow { ThunderException(BodyErrors.NOT_FOUND_BODY_PHOTO) }
        return BodyPhoto.from(entity)
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
        bodyPhotoEntity.update(bodyPhoto.isReviewCompleted, bodyPhoto.updatedAt)
    }

}