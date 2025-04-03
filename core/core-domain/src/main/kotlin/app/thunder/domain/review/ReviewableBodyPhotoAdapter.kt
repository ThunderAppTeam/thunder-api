package app.thunder.domain.review

import app.thunder.domain.review.command.CreateReviewableBodyPhotoCommand

interface ReviewableBodyPhotoAdapter {
    fun getAllByMemberId(memberId: Long, limit: Int? = null): List<ReviewableBodyPhoto>
    fun getAllByBodyPhotoId(bodyPhotoId: Long): List<ReviewableBodyPhoto>
    fun getFirstByMemberIds(memberIds: Collection<Long>): List<ReviewableBodyPhoto>
    fun deleteByMemberIdAndBodyPhotoId(memberId: Long, bodyPhotoId: Long)
    fun deleteAllByMemberId(memberId: Long)
    fun deleteAllByBodyPhotoId(bodyPhotoId: Long)
    fun deleteAllByMemberIdAndPhotoMemberId(memberId: Long, bodyPhotoMemberId: Long)
    fun saveAll(commands: Collection<CreateReviewableBodyPhotoCommand>)
}
