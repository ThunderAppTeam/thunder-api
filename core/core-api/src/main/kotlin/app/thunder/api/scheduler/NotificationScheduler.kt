package app.thunder.api.scheduler

import app.thunder.api.adapter.notification.NotificationAdapter
import app.thunder.api.domain.member.adapter.FcmTokenAdapter
import app.thunder.api.domain.member.adapter.MemberAdapter
import app.thunder.api.domain.photo.BodyPhotoAdapter
import app.thunder.api.domain.review.adapter.BodyReviewAdapter
import app.thunder.api.domain.review.adapter.ReviewableBodyPhotoAdapter
import app.thunder.domain.member.Member
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset.UTC
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class NotificationScheduler(
    private val memberAdapter: MemberAdapter,
    private val bodyPhotoAdapter: BodyPhotoAdapter,
    private val bodyReviewAdapter: BodyReviewAdapter,
    private val reviewableBodyPhotoAdapter: ReviewableBodyPhotoAdapter,
    private val fcmTokenAdapter: FcmTokenAdapter,
    private val notificationAdapter: NotificationAdapter,
) {

    companion object {
        private val TESTER_MEMBER_IDS = setOf<Long>(1, 2, 26, 32)
    }

    @Scheduled(cron = "0 0 21 * * *", zone = "Asia/Seoul")
    fun notifyReviewable() {
        val midNight = LocalDate.now()
            .atStartOfDay(ZoneId.of("Asia/Seoul"))
            .withZoneSameInstant(UTC)
            .toLocalDateTime()

        val memberIdToLatestReviewMap = bodyReviewAdapter.getMemberIdToLatestReviewMap()
        val allMembers = memberAdapter.getAllByReviewRequestNotifyTrue()
        val notificationTargetMembers = allMembers.asSequence()
            .filter { !TESTER_MEMBER_IDS.contains(it.memberId) }
            .filter { member ->
                val latestReview = memberIdToLatestReviewMap[member.memberId]
                latestReview == null || latestReview.createdAt.isBefore(midNight)
            }

        val memberIdSet = notificationTargetMembers.map(Member::memberId).toSet()
        val memberIdToFirstReviewableMap = reviewableBodyPhotoAdapter.getFirstByMemberIds(memberIdSet)
            .associateBy { it.memberId }

        val bodyPhotoIdSet = memberIdToFirstReviewableMap.values.map { it.bodyPhotoId }.toSet()
        val bodyPhotoMap = bodyPhotoAdapter.getAllById(bodyPhotoIdSet).associateBy { it.bodyPhotoId }
        val memberIdToFcmTokenMap = fcmTokenAdapter.getMemberIdToFcmTokenMap(memberIdSet)

        notificationTargetMembers
            .filter { memberIdToFirstReviewableMap.containsKey(it.memberId) }
            .forEach { targetMember ->
                val imageUrl = memberIdToFirstReviewableMap[targetMember.memberId]
                    ?.let { reviewable -> bodyPhotoMap[reviewable.bodyPhotoId] }
                    ?.imageUrl

                memberIdToFcmTokenMap[targetMember.memberId]
                    ?.let { fcmToken -> this.sendNotify(fcmToken, targetMember.nickname, imageUrl) }
            }
    }

    @Scheduled(cron = "0 0/30 9-23 * * *", zone = "Asia/Seoul")
    fun notifyReviewableForTest() {
        val before30Minutes = LocalDateTime.now().minusMinutes(30)

        val memberIdToLatestReviewMap = bodyReviewAdapter.getMemberIdToLatestReviewMap()
        val testers = memberAdapter.getAllByReviewRequestNotifyTrue()
            .filter { TESTER_MEMBER_IDS.contains(it.memberId) }
        val notificationTargetMembers = testers.filter { member ->
            val latestReview = memberIdToLatestReviewMap[member.memberId]
            latestReview == null || latestReview.createdAt.isBefore(before30Minutes)
        }

        val memberIdSet = notificationTargetMembers.map(Member::memberId).toSet()
        val memberIdToFirstReviewableMap = reviewableBodyPhotoAdapter.getFirstByMemberIds(memberIdSet)
            .associateBy { it.memberId }

        val bodyPhotoIdSet = memberIdToFirstReviewableMap.values.map { it.bodyPhotoId }.toSet()
        val bodyPhotoMap = bodyPhotoAdapter.getAllById(bodyPhotoIdSet).associateBy { it.bodyPhotoId }
        val memberIdToFcmTokenMap = fcmTokenAdapter.getMemberIdToFcmTokenMap(memberIdSet)

        notificationTargetMembers
            .filter { memberIdToFirstReviewableMap.containsKey(it.memberId) }
            .forEach { targetMember ->
                val imageUrl = memberIdToFirstReviewableMap[targetMember.memberId]
                    ?.let { reviewable -> bodyPhotoMap[reviewable.bodyPhotoId] }
                    ?.imageUrl

                memberIdToFcmTokenMap[targetMember.memberId]
                    ?.let { fcmToken -> this.sendNotify(fcmToken, targetMember.nickname, imageUrl) }
            }
    }

    private fun sendNotify(fcmToken: String, nickname: String, imageUrl: String?) {
        notificationAdapter.sendNotification(
            fcmToken = fcmToken,
            title = "새로운 눈바디 평가 요청 \uD83D\uDC40",
            body = "누군가 ${nickname}님께 눈바디 평가를 요청했어요.",
            imageUrl = imageUrl,
            routePath = "/"
        )
    }

}