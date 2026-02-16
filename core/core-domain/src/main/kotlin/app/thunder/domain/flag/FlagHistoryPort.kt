package app.thunder.domain.flag

data class FlagHistoryView(
    val memberId: Long,
    val bodyPhotoId: Long,
)

interface FlagHistoryPort {
    fun create(memberId: Long, bodyPhotoId: Long, flagReason: String, otherReason: String?)

    fun getAll(): List<FlagHistoryView>
}
