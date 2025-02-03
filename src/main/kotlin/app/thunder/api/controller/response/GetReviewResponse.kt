package app.thunder.api.controller.response

data class GetReviewResponse(
    val bodyPhotoId: Long,
    val imageUrl: String,
    val memberId: Long,
    val nickname: String,
    val age: Int,
)
