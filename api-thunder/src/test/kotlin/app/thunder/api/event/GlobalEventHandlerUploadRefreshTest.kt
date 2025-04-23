package app.thunder.api.event

import app.thunder.core.flag.repository.FlagHistoryRepository
import app.thunder.core.member.domain.Member
import app.thunder.core.member.repository.FcmTokenRepository
import app.thunder.core.member.repository.MemberRepository
import app.thunder.core.member.repository.MemberSettingRepository
import app.thunder.core.notification.client.NotificationClient
import app.thunder.core.photo.domain.BodyPhoto
import app.thunder.core.photo.repository.BodyPhotoRepository
import org.assertj.core.api.Assertions.assertThat
import org.instancio.Instancio
import org.instancio.Select.field
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.willThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension

@ExtendWith(OutputCaptureExtension::class)
class GlobalEventHandlerUploadRefreshTest {

    private val refresher = mock(ReviewableQueueRefresher::class.java)
    private val memberRepository = mock(MemberRepository::class.java)
    private val bodyPhotoRepository = mock(BodyPhotoRepository::class.java)
    private val flagHistoryRepository = mock(FlagHistoryRepository::class.java)
    private val handler = GlobalEventHandler(
        reviewableQueueRefresher = refresher,
        fcmTokenRepository = mock(FcmTokenRepository::class.java),
        notificationClient = mock(NotificationClient::class.java),
        memberSettingRepository = mock(MemberSettingRepository::class.java),
        memberRepository = memberRepository,
        bodyPhotoRepository = bodyPhotoRepository,
        flagHistoryRepository = flagHistoryRepository,
    )

    @Test
    @DisplayName("업로드 이벤트에서 한 회원의 큐 갱신이 실패해도 오류를 기록하고 나머지 회원의 큐는 계속 갱신한다")
    fun uploadRefreshContinuesAfterOneMemberFails(output: CapturedOutput) {
        // given
        val failingMember = member(FAILING_MEMBER_ID)
        val healthyMember = member(HEALTHY_MEMBER_ID)
        val photos = Instancio.createList(BodyPhoto::class.java)
        val flagCounts = mapOf(photos.first().bodyPhotoId to 1)
        given(memberRepository.getAll()).willReturn(listOf(failingMember, healthyMember))
        given(bodyPhotoRepository.getNotReviewCompletedAll()).willReturn(photos)
        given(flagHistoryRepository.getFlagCountByBodyPhotoId()).willReturn(flagCounts)
        willThrow(IllegalStateException("refresh broke")).given(refresher).refresh(FAILING_MEMBER_ID, photos, flagCounts)

        // when
        handler.handleReviewUploadEvent(ReviewUploadEvent(Instancio.create(Long::class.javaObjectType)))

        // then
        verify(refresher).refresh(FAILING_MEMBER_ID, photos, flagCounts)
        verify(refresher).refresh(HEALTHY_MEMBER_ID, photos, flagCounts)
        assertThat(output.all).contains("refresh failed - memberId: $FAILING_MEMBER_ID")
        assertThat(output.all).doesNotContain("refresh failed - memberId: $HEALTHY_MEMBER_ID")
    }

    private fun member(memberId: Long): Member =
        Instancio.of(Member::class.java).set(field(Member::class.java, "memberId"), memberId).create()

    companion object {
        private const val FAILING_MEMBER_ID = 900_501L
        private const val HEALTHY_MEMBER_ID = 900_502L
    }

}
