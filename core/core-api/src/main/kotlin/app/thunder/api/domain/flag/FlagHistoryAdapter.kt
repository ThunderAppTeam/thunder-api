package app.thunder.api.domain.flag

import app.thunder.api.exception.BodyErrors.ALREADY_FLAGGED
import app.thunder.api.exception.ThunderException
import app.thunder.api.func.nullIfBlank
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class FlagHistoryAdapter(
    private val flagHistoryRepository: FlagHistoryRepository,
) {

    @Transactional
    fun create(
        memberId: Long,
        bodyPhotoId: Long,
        flagReason: FlagReason,
        otherReason: String?,
    ) {
        if (flagHistoryRepository.existsByMemberIdAndBodyPhotoId(memberId, bodyPhotoId)) {
            throw ThunderException(ALREADY_FLAGGED)
        }

        val entity = FlagHistoryEntity.create(memberId = memberId,
                                              bodyPhotoId = bodyPhotoId,
                                              flagReason = flagReason,
                                              otherReason = otherReason?.nullIfBlank())
        flagHistoryRepository.save(entity)
    }

}