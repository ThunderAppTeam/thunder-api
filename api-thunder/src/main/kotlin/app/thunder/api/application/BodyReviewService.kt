package app.thunder.api.application

import app.thunder.api.controller.response.GetReviewableResponse
import app.thunder.api.event.RefreshReviewableEvent
import app.thunder.api.event.ReviewCompleteEvent
import app.thunder.libraries.errors.BodyErrors.ALREADY_REVIEWED
import app.thunder.libraries.errors.BodyErrors.NOT_FOUND_BODY_PHOTO
import app.thunder.libraries.errors.MemberErrors.NOT_FOUND_MEMBER
import app.thunder.libraries.errors.ThunderException
import app.thunder.core.member.repository.MemberRepository
import app.thunder.core.photo.repository.BodyPhotoRepository
import app.thunder.core.review.repository.BodyReviewRepository
import app.thunder.core.review.repository.DummyDeckRepository
import app.thunder.core.review.repository.ReviewableBodyPhotoRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BodyReviewService(
    private val memberRepository: MemberRepository,
    private val bodyPhotoRepository: BodyPhotoRepository,
    private val bodyReviewRepository: BodyReviewRepository,
    private val reviewableBodyPhotoRepository: ReviewableBodyPhotoRepository,
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val dummyDeckRepository: DummyDeckRepository,
) {

    @Transactional
    fun getReviewableBodyPhotoList(memberId: Long, size: Int): List<GetReviewableResponse> {
        val reviewableBodyPhotoList = reviewableBodyPhotoRepository.getAllByMemberId(memberId)
        val bodyPhotoIds = reviewableBodyPhotoList.take(size).map { it.bodyPhotoId }
        val bodyPhotoMap = bodyPhotoRepository.getAllById(bodyPhotoIds)
            .associateBy { it.bodyPhotoId }
        val memberIdSet = bodyPhotoMap.values.map { it.memberId }.toSet()
        val memberMap = memberRepository.getAllById(memberIdSet)
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
            return dummyDeckRepository.getAllByMemberId(memberId).take(size)
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
        val isDummy = dummyDeckRepository.deleteByMemberIdAndBodyPhotoId(memberId, bodyPhotoId)
        if (isDummy) {
            return
        }

        val bodyPhoto = bodyPhotoRepository.getById(bodyPhotoId)
            ?: throw ThunderException(NOT_FOUND_BODY_PHOTO)
        if (bodyReviewRepository.existsByBodyPhotoIdAndMemberId(bodyPhotoId, memberId)) {
            throw ThunderException(ALREADY_REVIEWED)
        }
        bodyPhoto.addReview(score)
        bodyPhotoRepository.update(bodyPhoto)

        val member = memberRepository.getById(memberId)
            ?: throw ThunderException(NOT_FOUND_MEMBER)
        bodyReviewRepository.create(bodyPhotoId, member.memberId, score)

        reviewableBodyPhotoRepository.deleteByMemberIdAndBodyPhotoId(memberId, bodyPhotoId)
        applicationEventPublisher.publishEvent(RefreshReviewableEvent(memberId))
        if (bodyPhoto.isReviewCompleted()) {
            val reviewCompleteEvent = ReviewCompleteEvent(bodyPhoto.memberId, bodyPhotoId, bodyPhoto.imageUrl)
            applicationEventPublisher.publishEvent(reviewCompleteEvent)
        }
    }

}
