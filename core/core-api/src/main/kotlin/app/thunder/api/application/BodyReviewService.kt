package app.thunder.api.application

import app.thunder.api.controller.response.GetReviewableResponse
import app.thunder.api.event.RefreshReviewableEvent
import app.thunder.api.event.ReviewCompleteEvent
import app.thunder.shared.errors.BodyErrors.ALREADY_REVIEWED
import app.thunder.shared.errors.BodyErrors.NOT_FOUND_BODY_PHOTO
import app.thunder.shared.errors.MemberErrors.NOT_FOUND_MEMBER
import app.thunder.shared.errors.ThunderException
import app.thunder.domain.member.MemberPort
import app.thunder.domain.photo.BodyPhotoPort
import app.thunder.domain.review.BodyReviewPort
import app.thunder.domain.review.DummyDeckPort
import app.thunder.domain.review.ReviewableBodyPhotoPort
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BodyReviewService(
    private val memberPort: MemberPort,
    private val bodyPhotoPort: BodyPhotoPort,
    private val bodyReviewPort: BodyReviewPort,
    private val reviewableBodyPhotoPort: ReviewableBodyPhotoPort,
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val dummyDeckPort: DummyDeckPort,
) {

    @Transactional
    fun getReviewableBodyPhotoList(memberId: Long, size: Int): List<GetReviewableResponse> {
        val reviewableBodyPhotoList = reviewableBodyPhotoPort.getAllByMemberId(memberId)
        val bodyPhotoIds = reviewableBodyPhotoList.take(size).map { it.bodyPhotoId }
        val bodyPhotoMap = bodyPhotoPort.getAllById(bodyPhotoIds)
            .associateBy { it.bodyPhotoId }
        val memberIdSet = bodyPhotoMap.values.map { it.memberId }.toSet()
        val memberMap = memberPort.getAllById(memberIdSet)
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
            return dummyDeckPort.getAllByMemberId(memberId).take(size)
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
        val isDummy = dummyDeckPort.deleteByMemberIdAndBodyPhotoId(memberId, bodyPhotoId)
        if (isDummy) {
            return
        }

        val bodyPhoto = bodyPhotoPort.getById(bodyPhotoId)
            ?: throw ThunderException(NOT_FOUND_BODY_PHOTO)
        if (bodyReviewPort.existsByBodyPhotoIdAndMemberId(bodyPhotoId, memberId)) {
            throw ThunderException(ALREADY_REVIEWED)
        }
        bodyPhoto.addReview(score)
        bodyPhotoPort.update(bodyPhoto)

        val member = memberPort.getById(memberId)
            ?: throw ThunderException(NOT_FOUND_MEMBER)
        bodyReviewPort.create(bodyPhotoId, member.memberId, score)

        reviewableBodyPhotoPort.deleteByMemberIdAndBodyPhotoId(memberId, bodyPhotoId)
        applicationEventPublisher.publishEvent(RefreshReviewableEvent(memberId))
        if (bodyPhoto.isReviewCompleted()) {
            val reviewCompleteEvent = ReviewCompleteEvent(bodyPhoto.memberId, bodyPhotoId, bodyPhoto.imageUrl)
            applicationEventPublisher.publishEvent(reviewCompleteEvent)
        }
    }

}
