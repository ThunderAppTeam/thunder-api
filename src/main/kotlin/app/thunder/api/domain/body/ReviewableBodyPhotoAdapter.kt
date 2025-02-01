package app.thunder.api.domain.body

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class ReviewableBodyPhotoAdapter(
    val reviewableBodyPhotoRepository: ReviewableBodyPhotoRepository,
) {

    @Transactional(readOnly = true)
    fun getAllByMemberId(memberId: Long): List<ReviewableBodyPhoto> {
        return reviewableBodyPhotoRepository.findAllByMemberId(memberId)
            .map(ReviewableBodyPhoto::from)
    }

    @Transactional
    fun create(memberId: Long, bodyPhotoId: Long, bodyPhotoMemberId: Long) {
        val entity = ReviewableBodyPhotoEntity.create(memberId, bodyPhotoId, bodyPhotoMemberId)
        reviewableBodyPhotoRepository.save(entity)
    }

    @Transactional
    fun deleteByMemberIdAndBodyPhotoId(memberId: Long, bodyPhotoId: Long) {
        reviewableBodyPhotoRepository.deleteByMemberIdAndBodyPhotoId(memberId, bodyPhotoId)
    }

}