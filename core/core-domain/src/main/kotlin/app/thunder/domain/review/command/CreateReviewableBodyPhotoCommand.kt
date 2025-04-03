package app.thunder.domain.review.command

data class CreateReviewableBodyPhotoCommand(
    val memberId: Long,
    val bodyPhotoId: Long,
    val bodyPhotoMemberId: Long,
)
