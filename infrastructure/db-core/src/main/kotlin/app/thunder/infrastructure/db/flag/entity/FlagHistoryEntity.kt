package app.thunder.infrastructure.db.flag.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Table(name = "flag_history")
@Entity
internal class FlagHistoryEntity private constructor(
    memberId: Long,
    bodyPhotoId: Long,
    flagReason: FlagReason,
    otherReason: String?,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "flag_history_id")
    val flagHistoryId: Long = 0

    @Column(name = "member_id", nullable = false)
    val memberId: Long = memberId

    @Column(name = "body_photo_id", nullable = false)
    val bodyPhotoId: Long = bodyPhotoId

    @Enumerated(EnumType.STRING)
    @Column(name = "flag_reason", nullable = false)
    val flagReason: FlagReason = flagReason

    @Column(name = "other_reason")
    val otherReason: String? = otherReason

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()

    companion object {
        fun create(
            memberId: Long,
            bodyPhotoId: Long,
            flagReason: FlagReason,
            otherReason: String?,
        ): FlagHistoryEntity {
            return FlagHistoryEntity(
                memberId = memberId,
                bodyPhotoId = bodyPhotoId,
                flagReason = flagReason,
                otherReason = otherReason,
            )
        }
    }

}

internal enum class FlagReason {
    SEXUAL_CONTENT,
    IRRELEVANT_BODY_CHECK,
    IMPERSONATION,
    VERBAL_ABUSE,
    CHILD_SEXUAL_ABUSE,
    OTHER,
}
