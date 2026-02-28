package app.thunder.infrastructure.db.flag

import app.thunder.domain.flag.FlagHistory
import app.thunder.domain.flag.FlagHistoryPort
import app.thunder.domain.flag.FlagReason
import app.thunder.infrastructure.db.flag.entity.FlagHistoryEntity
import app.thunder.infrastructure.db.flag.persistence.FlagHistoryJpaRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
internal class FlagHistoryAdapter(
    private val flagHistoryJpaRepository: FlagHistoryJpaRepository,
) : FlagHistoryPort {

    @Transactional
    override fun create(
        memberId: Long,
        bodyPhotoId: Long,
        flagReason: FlagReason,
        otherReason: String?,
    ) {
        val entity = FlagHistoryEntity.create(
            memberId = memberId,
            bodyPhotoId = bodyPhotoId,
            flagReason = flagReason,
            otherReason = otherReason?.takeIf { it.isNotBlank() },
        )
        flagHistoryJpaRepository.save(entity)
    }

    @Transactional(readOnly = true)
    override fun exists(memberId: Long, bodyPhotoId: Long): Boolean {
        return flagHistoryJpaRepository.existsByMemberIdAndBodyPhotoId(memberId, bodyPhotoId)
    }

    @Transactional(readOnly = true)
    override fun getAll(): List<FlagHistory> {
        return flagHistoryJpaRepository.findAll().map { entity ->
            FlagHistory(
                flagHistoryId = entity.flagHistoryId,
                memberId = entity.memberId,
                bodyPhotoId = entity.bodyPhotoId,
                flagReason = entity.flagReason,
                otherReason = entity.otherReason,
                createdAt = entity.createdAt,
            )
        }
    }

}
