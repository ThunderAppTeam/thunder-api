package app.thunder.domain.photo

import app.thunder.domain.member.Gender

interface BodyPhotoPort {
    fun getById(bodyPhotoId: Long): BodyPhoto?
    fun getAllByMemberId(memberId: Long): List<BodyPhoto>
    fun getNotReviewCompletedAll(): List<BodyPhoto>
    fun getAllById(bodyPhotoIds: Collection<Long>): List<BodyPhoto>
    fun getAllByGender(gender: Gender): List<BodyPhoto>
    fun countByGender(gender: Gender): Long
    fun countByGenderWithHigherScore(gender: Gender, score: Double): Long
    fun create(memberId: Long, imageUrl: String): BodyPhoto
    fun update(bodyPhoto: BodyPhoto)
    fun deleteById(bodyPhotoId: Long)
    fun deleteAllByMemberId(memberId: Long)
}
