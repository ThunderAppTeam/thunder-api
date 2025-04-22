package app.thunder.core.review.repository

import app.thunder.core.review.command.CreateReviewableBodyPhotoCommand
import app.thunder.core.review.domain.ReviewableBodyPhoto

interface ReviewableBodyPhotoRepository {
    fun getAllByMemberId(memberId: Long, limit: Int? = null): List<ReviewableBodyPhoto>
    fun exists(memberId: Long, bodyPhotoId: Long): Boolean
    fun lockQueue(memberId: Long)
    fun getAllByBodyPhotoId(bodyPhotoId: Long): List<ReviewableBodyPhoto>
    fun getFirstByMemberIds(memberIds: Collection<Long>): List<ReviewableBodyPhoto>
    fun deleteByMemberIdAndBodyPhotoId(memberId: Long, bodyPhotoId: Long)
    fun deleteAllByMemberId(memberId: Long)
    fun deleteAllByBodyPhotoId(bodyPhotoId: Long)
    fun deleteAllByMemberIdAndPhotoMemberId(memberId: Long, bodyPhotoMemberId: Long)
    fun saveAll(commands: Collection<CreateReviewableBodyPhotoCommand>)
}
