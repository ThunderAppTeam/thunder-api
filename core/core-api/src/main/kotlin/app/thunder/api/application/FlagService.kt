package app.thunder.api.application

import app.thunder.api.controller.response.GetFlagReasonResponse
import app.thunder.api.event.RefreshReviewableEvent
import app.thunder.domain.flag.FlagHistoryPort
import app.thunder.domain.flag.FlagReason
import app.thunder.domain.review.ReviewableBodyPhotoPort
import app.thunder.shared.errors.BodyErrors.ALREADY_FLAGGED
import app.thunder.shared.errors.ThunderException
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class FlagService(
    private val flagHistoryPort: FlagHistoryPort,
    private val reviewableBodyPhotoPort: ReviewableBodyPhotoPort,
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
        if (flagHistoryPort.exists(memberId, bodyPhotoId)) {
            throw ThunderException(ALREADY_FLAGGED)
        }

        flagHistoryPort.create(memberId = memberId,
                               bodyPhotoId = bodyPhotoId,
                               flagReason = flagReason,
                               otherReason = otherReason)

        reviewableBodyPhotoPort.deleteByMemberIdAndBodyPhotoId(memberId, bodyPhotoId)
        applicationEventPublisher.publishEvent(RefreshReviewableEvent(memberId))
    }

}
