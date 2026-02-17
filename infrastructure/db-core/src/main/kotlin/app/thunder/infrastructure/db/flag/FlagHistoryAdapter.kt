package app.thunder.infrastructure.db.flag

import app.thunder.domain.flag.FlagHistoryPort
import app.thunder.domain.flag.FlagHistoryView
import app.thunder.infrastructure.db.flag.entity.FlagHistoryEntity
import app.thunder.infrastructure.db.flag.entity.FlagReason
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
        flagReason: String,
        otherReason: String?,
    ) {
        if (flagHistoryJpaRepository.existsByMemberIdAndBodyPhotoId(memberId, bodyPhotoId)) {
            throw createAlreadyFlaggedException()
        }

        val reason = FlagReason.valueOf(flagReason)

        val entity = FlagHistoryEntity.create(
            memberId = memberId,
            bodyPhotoId = bodyPhotoId,
            flagReason = reason,
            otherReason = otherReason?.takeIf { it.isNotBlank() },
        )
        flagHistoryJpaRepository.save(entity)
    }

    @Transactional(readOnly = true)
    override fun getAll(): List<FlagHistoryView> {
        return flagHistoryJpaRepository.findAll().map { entity ->
            FlagHistoryView(
                memberId = entity.memberId,
                bodyPhotoId = entity.bodyPhotoId,
            )
        }
    }

    private fun createAlreadyFlaggedException(): RuntimeException {
        return try {
            val bodyErrorsClass = Class.forName("app.thunder.shared.errors.BodyErrors")
            val thunderExceptionClass = Class.forName("app.thunder.shared.errors.ThunderException")
            val alreadyFlagged = requireNotNull(bodyErrorsClass.enumConstants)
                .first { (it as Enum<*>).name == ALREADY_FLAGGED_ERROR_CODE_NAME }
            val constructor = thunderExceptionClass.declaredConstructors
                .first { it.parameterCount == 1 }
            constructor.newInstance(alreadyFlagged) as RuntimeException
        } catch (ex: Exception) {
            IllegalStateException(ALREADY_FLAGGED_ERROR_CODE_NAME, ex)
        }
    }

    companion object {
        private const val ALREADY_FLAGGED_ERROR_CODE_NAME = "ALREADY_FLAGGED"
    }

}
