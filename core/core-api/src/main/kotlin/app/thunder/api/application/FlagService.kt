package app.thunder.api.application

import app.thunder.api.controller.response.GetFlagReasonResponse
import app.thunder.api.event.RefreshReviewableEvent
import app.thunder.domain.flag.FlagHistoryPort
import app.thunder.domain.flag.FlagReason
import app.thunder.domain.review.ReviewableBodyPhotoPort
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class FlagService(
    private val flagHistoryPort: FlagHistoryPort,
    private val reviewableBodyPhotoAdapter: ReviewableBodyPhotoPort,
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
        flagHistoryPort.create(memberId = memberId,
                               bodyPhotoId = bodyPhotoId,
                               flagReason = flagReason.name,
                               otherReason = otherReason)
        reviewableBodyPhotoAdapter.deleteByMemberIdAndBodyPhotoId(memberId, bodyPhotoId)
        applicationEventPublisher.publishEvent(RefreshReviewableEvent(memberId))
    }

}
