package app.thunder.api.application

import app.thunder.api.controller.response.PostReviewRefreshResponse
import app.thunder.api.domain.photo.BodyPhotoAdapter
import app.thunder.api.domain.review.BodyReviewAdapter
import app.thunder.api.domain.body.ReviewRotationAdapter
import app.thunder.api.domain.member.MemberAdapter
import app.thunder.api.exception.BodyErrors
import app.thunder.api.exception.BodyErrors.ALREADY_REVIEWED
import app.thunder.api.exception.MemberErrors.NOT_FOUND_MEMBER
import app.thunder.api.exception.ThunderException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BodyReviewService(
    private val memberAdapter: MemberAdapter,
    private val bodyPhotoAdapter: BodyPhotoAdapter,
    private val bodyReviewAdapter: BodyReviewAdapter,
    private val reviewRotationAdapter: ReviewRotationAdapter,
) {

    @Transactional
    fun refreshReview(memberId: Long, refreshCount: Int): List<PostReviewRefreshResponse> {
        val bodyPhotoIdSet = linkedSetOf<Long>()
        val fetchSize = 5
        var reviewRotationId = 0L
        while (bodyPhotoIdSet.size < refreshCount) {
            val reviewRotations = reviewRotationAdapter
                .getAllByIdGteAndMemberIdNot(reviewRotationId, memberId, fetchSize)
            if (reviewRotations.isEmpty()) {
                break
            }
            reviewRotations.asSequence()
                .filter { !it.reviewedMemberIds.contains(memberId) }
                .take(refreshCount - bodyPhotoIdSet.size)
                .forEach { bodyPhotoIdSet.add(it.bodyPhotoId) }

            reviewRotationId += fetchSize
        }
        reviewRotationAdapter.refresh(bodyPhotoIdSet, memberId)

        val bodyPhotoMap = bodyPhotoAdapter.getAllById(bodyPhotoIdSet)
            .associateBy { it.bodyPhotoId }
        val memberIdSet = bodyPhotoMap.values.map { it.memberId }.toSet()
        val memberMap = memberAdapter.getAllById(memberIdSet)
            .associateBy { it.memberId }

        return bodyPhotoIdSet.map { bodyPhotoId ->
            val bodyPhoto = bodyPhotoMap[bodyPhotoId] ?: throw ThunderException(BodyErrors.NOT_FOUND_BODY_PHOTO)
            val member = memberMap[bodyPhoto.memberId] ?: throw ThunderException(NOT_FOUND_MEMBER)
            PostReviewRefreshResponse(bodyPhoto.bodyPhotoId,
                                      bodyPhoto.imageUrl,
                                      member.memberId,
                                      member.nickname,
                                      member.age)
        }
    }

    @Transactional
    fun review(bodyPhotoId: Long, memberId: Long, score: Int) {
        val bodyPhoto = bodyPhotoAdapter.getById(bodyPhotoId)
        if (bodyReviewAdapter.existsByBodyPhotoIdAndMemberId(bodyPhotoId, memberId)) {
            throw ThunderException(ALREADY_REVIEWED)
        }

        val bodyReviews = bodyReviewAdapter.getAllByBodyPhotoId(bodyPhotoId)
        val reviewCount = bodyReviews.size + 1
        if (reviewCount >= REVIEW_COMPLETE_COUNT) {
            bodyPhoto.completeReview()
        }

        val totalScore = bodyReviews.sumOf { it.score }.toDouble() + score
        val newReviewScore = totalScore / (bodyReviews.size + 1) * 2
        bodyPhoto.updateReviewScore(newReviewScore)
        bodyPhotoAdapter.update(bodyPhoto)

        val member = memberAdapter.getById(memberId)
        bodyReviewAdapter.create(bodyPhotoId, member.memberId, score)
    }

    companion object {
        private const val REVIEW_COMPLETE_COUNT = 20L
    }

}