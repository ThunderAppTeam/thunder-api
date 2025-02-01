package app.thunder.api.application

import app.thunder.api.domain.body.ReviewableBodyPhotoAdapter
import app.thunder.api.domain.flag.FlagHistoryAdapter
import app.thunder.api.domain.flag.FlagReason
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class FlagService(
    private val flagHistoryAdapter: FlagHistoryAdapter,
    private val reviewableBodyPhotoAdapter: ReviewableBodyPhotoAdapter,
) {

    @Transactional
    fun flagBodyPhoto(
        memberId: Long,
        bodyPhotoId: Long,
        flagReason: FlagReason,
        otherReason: String?,
    ) {
        reviewableBodyPhotoAdapter.deleteByMemberIdAndBodyPhotoId(memberId, bodyPhotoId)
        flagHistoryAdapter.create(memberId = memberId,
                                  bodyPhotoId = bodyPhotoId,
                                  flagReason = flagReason,
                                  otherReason = otherReason)
    }

}