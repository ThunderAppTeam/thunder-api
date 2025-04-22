package app.thunder.api.application

import app.thunder.api.controller.response.GetReviewableResponse
import app.thunder.api.event.RefreshReviewableEvent
import app.thunder.api.event.ReviewCompleteEvent
import app.thunder.libraries.errors.BodyErrors.ALREADY_REVIEWED
import app.thunder.libraries.errors.BodyErrors.ALREADY_REVIEW_COMPLETED
import app.thunder.libraries.errors.BodyErrors.NOT_FOUND_BODY_PHOTO
import app.thunder.libraries.errors.BodyErrors.NOT_REVIEWABLE_BODY_PHOTO
import app.thunder.libraries.errors.ThunderException
import app.thunder.core.member.repository.MemberRepository
import app.thunder.core.photo.domain.BodyPhoto.Companion.REVIEW_COMPLETE_COUNT
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
        val bodyPhotoIds = reviewableBodyPhotoRepository.getAllByMemberId(memberId)
            .map { it.bodyPhotoId }
        val bodyPhotoMap = bodyPhotoRepository.getAllById(bodyPhotoIds)
            .filterNot { it.isReviewCompleted() }
            .associateBy { it.bodyPhotoId }
        val reviewableBodyPhotos = bodyPhotoIds.mapNotNull { bodyPhotoMap[it] }.take(size)
        val memberMap = memberRepository.getAllById(reviewableBodyPhotos.map { it.memberId }.toSet())
            .associateBy { it.memberId }

        applicationEventPublisher.publishEvent(RefreshReviewableEvent(memberId))

        val reviewableDeck = reviewableBodyPhotos.mapNotNull { bodyPhoto ->
            val member = memberMap[bodyPhoto.memberId] ?: return@mapNotNull null
            GetReviewableResponse(bodyPhoto.bodyPhotoId,
                                  bodyPhoto.imageUrl,
                                  member.memberId,
                                  member.nickname,
                                  member.age)
        }
        if (reviewableDeck.isEmpty()) {
            reviewableBodyPhotoRepository.lockQueue(memberId)
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

        reviewableBodyPhotoRepository.lockQueue(memberId)
        if (bodyReviewRepository.existsByBodyPhotoIdAndMemberId(bodyPhotoId, memberId)) {
            throw ThunderException(ALREADY_REVIEWED)
        }
        if (!reviewableBodyPhotoRepository.exists(memberId, bodyPhotoId)) {
            throw ThunderException(NOT_REVIEWABLE_BODY_PHOTO)
        }
        if (!bodyPhotoRepository.addReview(bodyPhotoId, score)) {
            throw rejectedReviewException(bodyPhotoId)
        }
        bodyReviewRepository.create(bodyPhotoId, memberId, score)

        reviewableBodyPhotoRepository.deleteByMemberIdAndBodyPhotoId(memberId, bodyPhotoId)
        applicationEventPublisher.publishEvent(RefreshReviewableEvent(memberId))
        completeIfFinished(bodyPhotoId)
    }

    private fun completeIfFinished(bodyPhotoId: Long) {
        val bodyPhoto = bodyPhotoRepository.getById(bodyPhotoId)
            ?: throw ThunderException(NOT_FOUND_BODY_PHOTO)
        if (bodyPhoto.reviewCount != REVIEW_COMPLETE_COUNT) {
            return
        }
        reviewableBodyPhotoRepository.deleteAllByBodyPhotoId(bodyPhotoId)
        applicationEventPublisher.publishEvent(ReviewCompleteEvent(bodyPhoto.memberId, bodyPhotoId, bodyPhoto.imageUrl))
    }

    private fun rejectedReviewException(bodyPhotoId: Long): ThunderException {
        val errorCode = if (bodyPhotoRepository.getById(bodyPhotoId) == null) NOT_FOUND_BODY_PHOTO else ALREADY_REVIEW_COMPLETED
        return ThunderException(errorCode)
    }

}
