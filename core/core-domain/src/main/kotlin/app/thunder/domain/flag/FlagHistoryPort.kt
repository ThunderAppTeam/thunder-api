package app.thunder.domain.flag

interface FlagHistoryPort {
    fun create(memberId: Long, bodyPhotoId: Long, flagReason: FlagReason, otherReason: String?)

    fun exists(memberId: Long, bodyPhotoId: Long): Boolean

    fun getAll(): List<FlagHistory>
}
