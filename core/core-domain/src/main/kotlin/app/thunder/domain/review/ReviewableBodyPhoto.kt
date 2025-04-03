package app.thunder.domain.review

import java.time.LocalDateTime

class ReviewableBodyPhoto(
    val memberId: Long,
    val bodyPhotoId: Long,
    val bodyPhotoMemberId: Long,
    val createdAt: LocalDateTime,
)
