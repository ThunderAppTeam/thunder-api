package app.thunder.api.member

import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.willAnswer
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class MemberDeletionTest : MemberWebTest() {

    @BeforeEach
    fun createMembers() {
        insertMember(LEAVER_ID)
        insertMember(OTHER_ID)
        insertMember(THIRD_ID)
    }

    @Test
    @DisplayName("회원이 탈퇴하면 설정, FCM 토큰, 더미 덱, 차단 관계, 큐, 사진과 그 사진의 리뷰와 신고가 모두 사라진다")
    fun deleteRemovesMemberData() {
        // given
        val leaverPhoto = insertBodyPhoto(LEAVER_ID, imageUrl = LEAVER_PHOTO_URL)
        val leaverPhoto2 = insertBodyPhoto(LEAVER_ID, imageUrl = LEAVER_PHOTO_URL_2)
        val otherPhoto = insertBodyPhoto(OTHER_ID, imageUrl = OTHER_PHOTO_URL)
        insertBodyReview(leaverPhoto, OTHER_ID, 4)
        insertBodyReview(leaverPhoto2, THIRD_ID, 3)
        insertFlagHistory(leaverPhoto, THIRD_ID)
        insertBodyReview(otherPhoto, LEAVER_ID, 5)
        insertBodyReview(otherPhoto, THIRD_ID, 2)
        insertFlagHistory(otherPhoto, LEAVER_ID)
        insertSettings(LEAVER_ID)
        insertToken(LEAVER_ID, "leaver-token")
        insertToken(OTHER_ID, "other-token")
        insertDummyDeck(LEAVER_ID, 1L, THIRD_ID)
        insertDummyDeck(OTHER_ID, 1L, THIRD_ID)
        insertBlock(LEAVER_ID, OTHER_ID)
        insertBlock(OTHER_ID, LEAVER_ID)
        insertBlock(OTHER_ID, THIRD_ID)
        insertQueue(LEAVER_ID, otherPhoto, OTHER_ID)
        insertQueue(OTHER_ID, leaverPhoto, LEAVER_ID)
        insertQueue(THIRD_ID, otherPhoto, OTHER_ID)

        // when
        val result = mockMvc.perform(deletionRequest(LEAVER_ID))

        // then
        result.andExpect(status().isOk)
        assertThat(count("member", "member_id = ?", LEAVER_ID)).isEqualTo(0)
        assertThat(count("deleted_member", "member_id = ?", LEAVER_ID)).isEqualTo(1)
        assertThat(count("member_setting", "member_id = ?", LEAVER_ID)).isEqualTo(0)
        assertThat(count("member_fcm_token", "member_id = ?", LEAVER_ID)).isEqualTo(0)
        assertThat(count("dummy_deck", "member_id = ?", LEAVER_ID)).isEqualTo(0)
        assertThat(count("member_block_relation", "member_id = ? OR blocked_member_id = ?", LEAVER_ID, LEAVER_ID))
            .isEqualTo(0)
        assertThat(count("reviewable_body_photo", "member_id = ? OR body_photo_member_id = ?", LEAVER_ID, LEAVER_ID))
            .isEqualTo(0)
        assertThat(count("body_photo", "member_id = ?", LEAVER_ID)).isEqualTo(0)
        assertThat(count("body_review", "body_photo_id IN (?, ?)", leaverPhoto, leaverPhoto2)).isEqualTo(0)
        assertThat(count("flag_history", "body_photo_id = ?", leaverPhoto)).isEqualTo(0)
    }

    @Test
    @DisplayName("탈퇴해도 다른 회원의 사진에 남긴 리뷰와 신고, 다른 회원의 데이터는 그대로 남는다")
    fun deleteKeepsOtherMembersData() {
        // given
        val otherPhoto = insertBodyPhoto(OTHER_ID)
        insertBodyReview(otherPhoto, LEAVER_ID, 5)
        insertBodyReview(otherPhoto, THIRD_ID, 2)
        insertFlagHistory(otherPhoto, LEAVER_ID)
        insertToken(OTHER_ID, "other-token")
        insertSettings(OTHER_ID)
        insertDummyDeck(OTHER_ID, 1L, THIRD_ID)
        insertBlock(OTHER_ID, THIRD_ID)
        insertQueue(THIRD_ID, otherPhoto, OTHER_ID)

        // when
        mockMvc.perform(deletionRequest(LEAVER_ID)).andExpect(status().isOk)

        // then
        assertThat(count("body_photo", "body_photo_id = ?", otherPhoto)).isEqualTo(1)
        assertThat(count("body_review", "body_photo_id = ?", otherPhoto)).isEqualTo(2)
        assertThat(count("flag_history", "body_photo_id = ?", otherPhoto)).isEqualTo(1)
        assertThat(count("member_fcm_token", "member_id = ?", OTHER_ID)).isEqualTo(1)
        assertThat(count("member_setting", "member_id = ?", OTHER_ID)).isEqualTo(1)
        assertThat(count("dummy_deck", "member_id = ?", OTHER_ID)).isEqualTo(1)
        assertThat(count("member_block_relation", "member_id = ?", OTHER_ID)).isEqualTo(1)
        assertThat(count("reviewable_body_photo", "member_id = ?", THIRD_ID)).isEqualTo(1)
    }

    @Test
    @DisplayName("탈퇴하면 커밋된 뒤에 탈퇴 회원의 사진 S3 객체만 지운다")
    fun deleteRemovesPhotoObjectsAfterCommit() {
        // given
        val leaverPhoto = insertBodyPhoto(LEAVER_ID, imageUrl = LEAVER_PHOTO_URL)
        insertBodyPhoto(LEAVER_ID, imageUrl = LEAVER_PHOTO_URL_2)
        insertBodyPhoto(OTHER_ID, imageUrl = OTHER_PHOTO_URL)
        val rowCountWhenDeleting = AtomicInteger(-1)
        willAnswer {
            rowCountWhenDeleting.set(count("body_photo", "body_photo_id = ?", leaverPhoto))
        }.given(bodyPhotoStorageClient).delete(LEAVER_PHOTO_URL)

        // when
        mockMvc.perform(deletionRequest(LEAVER_ID)).andExpect(status().isOk)

        // then
        verify(bodyPhotoStorageClient).delete(LEAVER_PHOTO_URL)
        verify(bodyPhotoStorageClient).delete(LEAVER_PHOTO_URL_2)
        verify(bodyPhotoStorageClient, never()).delete(OTHER_PHOTO_URL)
        assertThat(rowCountWhenDeleting.get()).isEqualTo(0)
    }

    @Test
    @DisplayName("설정, 토큰, 사진이 없는 회원도 탈퇴할 수 있다")
    fun deleteMemberWithoutRelatedData() {
        // given

        // when
        val result = mockMvc.perform(deletionRequest(LEAVER_ID))

        // then
        result.andExpect(status().isOk)
        assertThat(count("member", "member_id = ?", LEAVER_ID)).isEqualTo(0)
        assertThat(count("deleted_member", "member_id = ?", LEAVER_ID)).isEqualTo(1)
    }

    @Test
    @DisplayName("탈퇴하는 회원의 큐 잠금이 잡혀 있으면 잠금이 풀릴 때까지 탈퇴가 끝나지 않는다")
    fun deleteWaitsForOwnQueueLock() {
        // given
        var statusWhileLocked: CompletableFuture<Int>? = null

        // when
        whileHoldingQueueLock(LEAVER_ID) {
            statusWhileLocked = CompletableFuture.supplyAsync {
                mockMvc.perform(deletionRequest(LEAVER_ID)).andReturn().response.status
            }
            Thread.sleep(BLOCKED_WAIT_MILLIS)

            // then
            assertThat(statusWhileLocked!!.isDone).isFalse()
        }
        assertThat(statusWhileLocked!!.get(15, TimeUnit.SECONDS)).isEqualTo(200)
    }

    companion object {
        private const val LEAVER_ID = 900_701L
        private const val OTHER_ID = 900_702L
        private const val THIRD_ID = 900_703L
        private const val BUCKET_URL = "https://test-bucket.s3.ap-northeast-2.amazonaws.com"
        private const val LEAVER_PHOTO_URL = "$BUCKET_URL/$LEAVER_ID/body_photo/a.jpg"
        private const val LEAVER_PHOTO_URL_2 = "$BUCKET_URL/$LEAVER_ID/body_photo/b.jpg"
        private const val OTHER_PHOTO_URL = "$BUCKET_URL/$OTHER_ID/body_photo/c.jpg"
    }

}
