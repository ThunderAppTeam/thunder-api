package app.thunder.core.photo.repository

import app.thunder.core.member.domain.Gender
import app.thunder.core.photo.domain.BodyPhoto
import java.time.LocalDateTime

interface BodyPhotoRepository {
    fun getById(bodyPhotoId: Long): BodyPhoto?
    fun getAllByMemberId(memberId: Long): List<BodyPhoto>
    fun getNotReviewCompletedAll(): List<BodyPhoto>
    fun getAllById(bodyPhotoIds: Collection<Long>): List<BodyPhoto>
    fun countReviewCompletedByGender(gender: Gender, since: LocalDateTime): Long
    fun countReviewCompletedByGenderWithHigherAverageScore(gender: Gender, averageScore: Double, since: LocalDateTime): Long
    fun create(memberId: Long, imageUrl: String): BodyPhoto
    fun addReview(bodyPhotoId: Long, score: Int): Boolean
    fun update(bodyPhoto: BodyPhoto)
    fun deleteById(bodyPhotoId: Long)
    fun deleteAllByMemberId(memberId: Long)
}
