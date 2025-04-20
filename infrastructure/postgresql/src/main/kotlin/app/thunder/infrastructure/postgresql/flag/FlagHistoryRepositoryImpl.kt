package app.thunder.infrastructure.postgresql.flag

import app.thunder.core.flag.domain.FlagHistory
import app.thunder.core.flag.repository.FlagHistoryRepository
import app.thunder.core.flag.domain.FlagReason
import app.thunder.infrastructure.postgresql.flag.entity.FlagHistoryEntity
import app.thunder.infrastructure.postgresql.flag.persistence.FlagHistoryJpaRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
internal class FlagHistoryRepositoryImpl(
    private val flagHistoryJpaRepository: FlagHistoryJpaRepository,
) : FlagHistoryRepository {

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

    @Transactional(readOnly = true)
    override fun getAllByMemberId(memberId: Long): List<FlagHistory> {
        return flagHistoryJpaRepository.findAllByMemberId(memberId).map { entity ->
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

    @Transactional(readOnly = true)
    override fun getFlagCountByBodyPhotoId(): Map<Long, Int> {
        return flagHistoryJpaRepository.findAll()
            .groupingBy { it.bodyPhotoId }
            .eachCount()
    }

}
