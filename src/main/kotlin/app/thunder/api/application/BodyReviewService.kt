package app.thunder.api.application

import app.thunder.api.application.SupplyReviewableEventHandler.Companion.REVIEWABLE_QUEUE_MINIMUM_SIZE
import app.thunder.api.controller.response.PostReviewRefreshResponse
import app.thunder.api.domain.body.ReviewRotationAdapter
import app.thunder.api.domain.body.ReviewableBodyPhotoAdapter
import app.thunder.api.domain.member.MemberAdapter
import app.thunder.api.domain.photo.BodyPhotoAdapter
import app.thunder.api.domain.review.BodyReviewAdapter
import app.thunder.api.exception.BodyErrors.ALREADY_REVIEWED
import app.thunder.api.exception.BodyErrors.NOT_FOUND_BODY_PHOTO
import app.thunder.api.exception.MemberErrors.NOT_FOUND_MEMBER
import app.thunder.api.exception.ThunderException
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BodyReviewService(
    private val memberAdapter: MemberAdapter,
    private val bodyPhotoAdapter: BodyPhotoAdapter,
    private val bodyReviewAdapter: BodyReviewAdapter,
    private val reviewRotationAdapter: ReviewRotationAdapter,
    private val reviewableBodyPhotoAdapter: ReviewableBodyPhotoAdapter,
    private val applicationEventPublisher: ApplicationEventPublisher,
) {

    @Transactional
    fun getReviewableBodyPhotoList(memberId: Long, size: Int): List<PostReviewRefreshResponse> {
        val reviewableBodyPhotoList = reviewableBodyPhotoAdapter.getAllByMemberId(memberId)
        val bodyPhotoIds = reviewableBodyPhotoList.take(size).map { it.bodyPhotoId }
        val bodyPhotoMap = bodyPhotoAdapter.getAllById(bodyPhotoIds)
            .associateBy { it.bodyPhotoId }
        val memberIdSet = bodyPhotoMap.values.map { it.memberId }.toSet()
        val memberMap = memberAdapter.getAllById(memberIdSet)
            .associateBy { it.memberId }

        return bodyPhotoIds.map { bodyPhotoId ->
            val bodyPhoto = bodyPhotoMap[bodyPhotoId] ?: throw ThunderException(NOT_FOUND_BODY_PHOTO)
            val member = memberMap[bodyPhoto.memberId] ?: throw ThunderException(NOT_FOUND_MEMBER)
            PostReviewRefreshResponse(bodyPhoto.bodyPhotoId,
                                      bodyPhoto.imageUrl,
                                      member.memberId,
                                      member.nickname,
                                      member.age)
        }
    }

    @Deprecated("replaced by getReviewableBodyPhotoList()")
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
            val bodyPhoto = bodyPhotoMap[bodyPhotoId] ?: throw ThunderException(NOT_FOUND_BODY_PHOTO)
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

        bodyPhoto.addReview(score)
        bodyPhotoAdapter.update(bodyPhoto)

        val member = memberAdapter.getById(memberId)
        bodyReviewAdapter.create(bodyPhotoId, member.memberId, score)

        reviewableBodyPhotoAdapter.deleteByMemberIdAndBodyPhotoId(memberId, bodyPhotoId)
        val reviewableQueue = reviewableBodyPhotoAdapter.getAllByMemberId(memberId)
        if (reviewableQueue.size <= REVIEWABLE_QUEUE_MINIMUM_SIZE) {
            applicationEventPublisher.publishEvent(SupplyReviewableEvent(memberId))
        }
    }

}