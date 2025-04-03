package app.thunder.api.application

import app.thunder.api.controller.response.GetReviewableResponse
import app.thunder.api.domain.member.adapter.MemberAdapter
import app.thunder.api.event.RefreshReviewableEvent
import app.thunder.api.event.ReviewCompleteEvent
import app.thunder.api.exception.BodyErrors.ALREADY_REVIEWED
import app.thunder.api.exception.BodyErrors.NOT_FOUND_BODY_PHOTO
import app.thunder.api.exception.MemberErrors.NOT_FOUND_MEMBER
import app.thunder.api.exception.ThunderException
import app.thunder.domain.photo.BodyPhotoAdapter
import app.thunder.domain.review.BodyReviewAdapter
import app.thunder.domain.review.DummyDeckAdapter
import app.thunder.domain.review.ReviewableBodyPhotoAdapter
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BodyReviewService(
    private val memberAdapter: MemberAdapter,
    private val bodyPhotoAdapter: BodyPhotoAdapter,
    private val bodyReviewAdapter: BodyReviewAdapter,
    private val reviewableBodyPhotoAdapter: ReviewableBodyPhotoAdapter,
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val dummyDeckAdapter: DummyDeckAdapter,
) {

    @Transactional
    fun getReviewableBodyPhotoList(memberId: Long, size: Int): List<GetReviewableResponse> {
        val reviewableBodyPhotoList = reviewableBodyPhotoAdapter.getAllByMemberId(memberId)
        val bodyPhotoIds = reviewableBodyPhotoList.take(size).map { it.bodyPhotoId }
        val bodyPhotoMap = bodyPhotoAdapter.getAllById(bodyPhotoIds)
            .associateBy { it.bodyPhotoId }
        val memberIdSet = bodyPhotoMap.values.map { it.memberId }.toSet()
        val memberMap = memberAdapter.getAllById(memberIdSet)
            .associateBy { it.memberId }

        applicationEventPublisher.publishEvent(RefreshReviewableEvent(memberId))

        val reviewableDeck = bodyPhotoIds.mapNotNull { bodyPhotoId ->
            val bodyPhoto = bodyPhotoMap[bodyPhotoId] ?: return@mapNotNull null
            val member = memberMap[bodyPhoto.memberId] ?: return@mapNotNull null
            GetReviewableResponse(bodyPhoto.bodyPhotoId,
                                  bodyPhoto.imageUrl,
                                  member.memberId,
                                  member.nickname,
                                  member.age)
        }
        if (reviewableDeck.isEmpty()) {
            return dummyDeckAdapter.getAllByMemberId(memberId).take(size)
                .map {
                    GetReviewableResponse(
                        bodyPhotoId = it.bodyPhotoId,
                        imageUrl = it.imageUrl,
                        memberId = it.bodyPhotoMemberId,
                        nickname = it.nickname,
                        age = it.age,
                    )
                }
        }
        return reviewableDeck
    }

    @Transactional
    fun review(bodyPhotoId: Long, memberId: Long, score: Int) {
        val isDummy = dummyDeckAdapter.deleteByMemberIdAndBodyPhotoId(memberId, bodyPhotoId)
        if (isDummy) {
            return
        }

        val bodyPhoto = bodyPhotoAdapter.getById(bodyPhotoId)
            ?: throw ThunderException(NOT_FOUND_BODY_PHOTO)
        if (bodyReviewAdapter.existsByBodyPhotoIdAndMemberId(bodyPhotoId, memberId)) {
            throw ThunderException(ALREADY_REVIEWED)
        }
        bodyPhoto.addReview(score)
        bodyPhotoAdapter.update(bodyPhoto)

        val member = memberAdapter.getById(memberId)
            ?: throw ThunderException(NOT_FOUND_MEMBER)
        bodyReviewAdapter.create(bodyPhotoId, member.memberId, score)

        reviewableBodyPhotoAdapter.deleteByMemberIdAndBodyPhotoId(memberId, bodyPhotoId)
        applicationEventPublisher.publishEvent(RefreshReviewableEvent(memberId))
        if (bodyPhoto.isReviewCompleted()) {
            val reviewCompleteEvent = ReviewCompleteEvent(bodyPhoto.memberId, bodyPhotoId, bodyPhoto.imageUrl)
            applicationEventPublisher.publishEvent(reviewCompleteEvent)
        }
    }

}