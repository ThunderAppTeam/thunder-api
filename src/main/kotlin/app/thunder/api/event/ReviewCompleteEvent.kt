package app.thunder.api.event

data class ReviewCompleteEvent(
    val memberId: Long,
    val bodyPhotoId: Long,
    val imageUrl: String,
)