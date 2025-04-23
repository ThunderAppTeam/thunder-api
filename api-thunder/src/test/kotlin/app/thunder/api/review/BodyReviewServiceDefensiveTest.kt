package app.thunder.api.review

import app.thunder.api.application.BodyReviewService
import app.thunder.core.member.repository.MemberRepository
import app.thunder.core.photo.repository.BodyPhotoRepository
import app.thunder.core.review.repository.BodyReviewRepository
import app.thunder.core.review.repository.DummyDeckRepository
import app.thunder.core.review.repository.ReviewableBodyPhotoRepository
import app.thunder.libraries.errors.BodyErrors.NOT_FOUND_BODY_PHOTO
import app.thunder.libraries.errors.ThunderException
import org.assertj.core.api.Assertions.assertThat
import org.instancio.Instancio
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.springframework.context.ApplicationEventPublisher

class BodyReviewServiceDefensiveTest {

    private val bodyPhotoRepository = mock(BodyPhotoRepository::class.java)
    private val bodyReviewRepository = mock(BodyReviewRepository::class.java)
    private val reviewableBodyPhotoRepository = mock(ReviewableBodyPhotoRepository::class.java)
    private val dummyDeckRepository = mock(DummyDeckRepository::class.java)
    private val service = BodyReviewService(
        memberRepository = mock(MemberRepository::class.java),
        bodyPhotoRepository = bodyPhotoRepository,
        bodyReviewRepository = bodyReviewRepository,
        reviewableBodyPhotoRepository = reviewableBodyPhotoRepository,
        applicationEventPublisher = mock(ApplicationEventPublisher::class.java),
        dummyDeckRepository = dummyDeckRepository,
    )

    @Test
    @DisplayName("점수를 반영한 직후 사진을 다시 읽을 수 없으면 NOT_FOUND_BODY_PHOTO 예외가 발생한다")
    fun photoVanishesAfterScoreIsApplied() {
        // given
        val bodyPhotoId = Instancio.create(Long::class.javaObjectType)
        val memberId = Instancio.create(Long::class.javaObjectType)
        given(dummyDeckRepository.deleteByMemberIdAndBodyPhotoId(memberId, bodyPhotoId)).willReturn(false)
        given(bodyReviewRepository.existsByBodyPhotoIdAndMemberId(bodyPhotoId, memberId)).willReturn(false)
        given(reviewableBodyPhotoRepository.exists(memberId, bodyPhotoId)).willReturn(true)
        given(bodyPhotoRepository.addReview(bodyPhotoId, SCORE)).willReturn(true)
        given(bodyPhotoRepository.getById(bodyPhotoId)).willReturn(null)

        // when
        val thrown = runCatching { service.review(bodyPhotoId, memberId, SCORE) }.exceptionOrNull()

        // then
        assertThat(thrown).isInstanceOf(ThunderException::class.java)
        assertThat((thrown as ThunderException).errorCode).isEqualTo(NOT_FOUND_BODY_PHOTO)
    }

    @Test
    @DisplayName("점수 반영이 거절됐는데 사진도 없으면 ALREADY_REVIEW_COMPLETED가 아니라 NOT_FOUND_BODY_PHOTO 예외가 발생한다")
    fun rejectedReviewOfMissingPhoto() {
        // given
        val bodyPhotoId = Instancio.create(Long::class.javaObjectType)
        val memberId = Instancio.create(Long::class.javaObjectType)
        given(dummyDeckRepository.deleteByMemberIdAndBodyPhotoId(memberId, bodyPhotoId)).willReturn(false)
        given(bodyReviewRepository.existsByBodyPhotoIdAndMemberId(bodyPhotoId, memberId)).willReturn(false)
        given(reviewableBodyPhotoRepository.exists(memberId, bodyPhotoId)).willReturn(true)
        given(bodyPhotoRepository.addReview(bodyPhotoId, SCORE)).willReturn(false)
        given(bodyPhotoRepository.getById(bodyPhotoId)).willReturn(null)

        // when
        val thrown = runCatching { service.review(bodyPhotoId, memberId, SCORE) }.exceptionOrNull()

        // then
        assertThat(thrown).isInstanceOf(ThunderException::class.java)
        assertThat((thrown as ThunderException).errorCode).isEqualTo(NOT_FOUND_BODY_PHOTO)
        verify(bodyReviewRepository, never()).create(bodyPhotoId, memberId, SCORE)
    }

    companion object {
        private const val SCORE = 4
    }

}
