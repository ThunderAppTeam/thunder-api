package app.thunder.api.domain.flag

import app.thunder.api.exception.BodyErrors.ALREADY_FLAGGED
import app.thunder.api.exception.ThunderException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class FlagHistoryAdapter(
    private val flagHistoryRepository: FlagHistoryRepository,
) {

    @Transactional(readOnly = true)
    fun getAllByMemberId(memberId: Long): List<FlagHistory> {
        return flagHistoryRepository.findAllByMemberId(memberId)
            .map(FlagHistory::from)
    }

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
                                              otherReason = otherReason)
        flagHistoryRepository.save(entity)
    }

}