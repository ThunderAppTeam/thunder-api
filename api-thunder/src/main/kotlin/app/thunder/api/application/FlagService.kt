package app.thunder.api.application

import app.thunder.api.controller.response.GetFlagReasonResponse
import app.thunder.api.event.RefreshReviewableEvent
import app.thunder.core.flag.repository.FlagHistoryRepository
import app.thunder.core.flag.domain.FlagReason
import app.thunder.core.review.repository.ReviewableBodyPhotoRepository
import app.thunder.libraries.errors.BodyErrors.ALREADY_FLAGGED
import app.thunder.libraries.errors.ThunderException
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class FlagService(
    private val flagHistoryRepository: FlagHistoryRepository,
    private val reviewableBodyPhotoRepository: ReviewableBodyPhotoRepository,
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
        if (flagHistoryRepository.exists(memberId, bodyPhotoId)) {
            throw ThunderException(ALREADY_FLAGGED)
        }

        flagHistoryRepository.create(memberId = memberId,
                               bodyPhotoId = bodyPhotoId,
                               flagReason = flagReason,
                               otherReason = otherReason)

        reviewableBodyPhotoRepository.deleteByMemberIdAndBodyPhotoId(memberId, bodyPhotoId)
        applicationEventPublisher.publishEvent(RefreshReviewableEvent(memberId))
    }

}
