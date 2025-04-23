package app.thunder.api.review

import java.time.LocalDateTime
import org.assertj.core.api.Assertions.assertThat
import org.instancio.Instancio
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito.mockingDetails
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@ExtendWith(OutputCaptureExtension::class)
class ReviewCompleteNotificationTest : BodyReviewWebTest() {

    @BeforeEach
    fun createMembers() {
        insertMember(OWNER_ID)
        insertMember(REVIEWER_ID)
    }

    @Test
    @DisplayName("10번째 리뷰로 완료됐는데 업로더에게 FCM 토큰이 없으면 알림을 보내지 않고 오류 로그를 남긴다")
    fun completionWithoutFcmToken(output: CapturedOutput) {
        // given
        val bodyPhotoId = insertBodyPhoto(OWNER_ID, reviewCount = 9, totalReviewScore = 36.0)
        insertQueue(REVIEWER_ID, bodyPhotoId, OWNER_ID)

        // when
        val result = mockMvc.perform(reviewRequest(REVIEWER_ID, bodyPhotoId))
        awaitAsyncIdle()

        // then
        result.andExpect(status().isOk)
        assertThat(reviewCount(bodyPhotoId)).isEqualTo(10)
        assertThat(notificationCount()).isEqualTo(0)
        assertThat(output.all).contains("notifyReviewComplete failed: not found fcm token - memberId: $OWNER_ID")
    }

    @Test
    @DisplayName("10번째 리뷰로 완료되고 업로더가 알림을 켜 둔 상태면 완료된 사진의 결과 경로로 알림을 보낸다")
    fun completionNotifiesOwnerWhoEnabledNotification() {
        // given
        val token = Instancio.create(String::class.java)
        insertSettings(OWNER_ID, reviewCompleteNotify = true)
        insertToken(OWNER_ID, token)
        val imageUrl = "https://test-bucket.s3.ap-northeast-2.amazonaws.com/$OWNER_ID/body_photo/done.jpg"
        val bodyPhotoId = insertBodyPhoto(OWNER_ID, reviewCount = 9, totalReviewScore = 36.0, imageUrl = imageUrl)
        insertQueue(REVIEWER_ID, bodyPhotoId, OWNER_ID)

        // when
        val result = mockMvc.perform(reviewRequest(REVIEWER_ID, bodyPhotoId))
        awaitAsyncIdle()

        // then
        result.andExpect(status().isOk)
        val arguments = sendNotificationArguments().single()
        assertThat(arguments[0]).isEqualTo(token)
        assertThat(arguments[3]).isEqualTo(imageUrl)
        assertThat(arguments[4]).isEqualTo("/bodyCheck/$bodyPhotoId")
    }

    private fun insertSettings(memberId: Long, reviewCompleteNotify: Boolean) {
        jdbcTemplate.update(
            "INSERT INTO member_setting (member_id, settings, created_at) VALUES (?, ?::jsonb, ?)",
            memberId,
            """{"reviewCompleteNotify":$reviewCompleteNotify,"reviewRequestNotify":true,"marketingAgreement":false}""",
            LocalDateTime.now(),
        )
    }

    private fun insertToken(memberId: Long, token: String) {
        jdbcTemplate.update(
            "INSERT INTO member_fcm_token (member_id, fcm_token, created_at) VALUES (?, ?, ?)",
            memberId, token, LocalDateTime.now(),
        )
    }

    private fun sendNotificationArguments(): List<Array<Any?>> =
        mockingDetails(notificationClient).invocations.filter { it.method.name == "sendNotification" }.map { it.arguments }

    companion object {
        private const val OWNER_ID = 900_431L
        private const val REVIEWER_ID = 900_432L
    }

}
