package app.thunder.api.application

import app.thunder.api.controller.response.GetFlagReasonResponse
import app.thunder.api.event.RefreshReviewableEvent
import app.thunder.core.flag.repository.FlagHistoryRepository
import app.thunder.core.flag.domain.FlagReason
import app.thunder.core.photo.repository.BodyPhotoRepository
import app.thunder.core.review.repository.DummyDeckRepository
import app.thunder.core.review.repository.ReviewableBodyPhotoRepository
import app.thunder.libraries.errors.BodyErrors.ALREADY_FLAGGED
import app.thunder.libraries.errors.BodyErrors.CANNOT_FLAG_OWN_PHOTO
import app.thunder.libraries.errors.BodyErrors.NOT_FOUND_BODY_PHOTO
import app.thunder.libraries.errors.BodyErrors.NOT_REVIEWABLE_BODY_PHOTO
import app.thunder.libraries.errors.ThunderException
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class FlagService(
    private val flagHistoryRepository: FlagHistoryRepository,
    private val bodyPhotoRepository: BodyPhotoRepository,
    private val reviewableBodyPhotoRepository: ReviewableBodyPhotoRepository,
    private val dummyDeckRepository: DummyDeckRepository,
    private val applicationEventPublisher: ApplicationEventPublisher,
) {

    fun getAllByCountryCode(countryCode: String): List<GetFlagReasonResponse> {
        return FlagReason.entries.map {
            when (countryCode) {
                "KR" -> GetFlagReasonResponse(it.name, it.descriptionKR)
                else -> GetFlagReasonResponse(it.name, it.descriptionKR)
            }
        }
    }

    @Transactional
    fun flagBodyPhoto(
        memberId: Long,
        bodyPhotoId: Long,
        flagReason: FlagReason,
        otherReason: String?,
    ) {
        reviewableBodyPhotoRepository.lockQueue(memberId)
        val bodyPhoto = bodyPhotoRepository.getById(bodyPhotoId)
            ?: throw ThunderException(NOT_FOUND_BODY_PHOTO)
        if (bodyPhoto.isUploader(memberId)) {
            throw ThunderException(CANNOT_FLAG_OWN_PHOTO)
        }
        if (flagHistoryRepository.exists(memberId, bodyPhotoId)) {
            throw ThunderException(ALREADY_FLAGGED)
        }
        if (!isShownTo(memberId, bodyPhotoId)) {
            throw ThunderException(NOT_REVIEWABLE_BODY_PHOTO)
        }

        flagHistoryRepository.create(memberId = memberId,
                               bodyPhotoId = bodyPhotoId,
                               flagReason = flagReason,
                               otherReason = otherReason)

        reviewableBodyPhotoRepository.deleteByMemberIdAndBodyPhotoId(memberId, bodyPhotoId)
        applicationEventPublisher.publishEvent(RefreshReviewableEvent(memberId))
    }

    private fun isShownTo(memberId: Long, bodyPhotoId: Long): Boolean {
        return reviewableBodyPhotoRepository.exists(memberId, bodyPhotoId) ||
            dummyDeckRepository.exists(memberId, bodyPhotoId)
    }

}
