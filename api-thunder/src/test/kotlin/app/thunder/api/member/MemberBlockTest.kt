package app.thunder.api.member

import app.thunder.api.ConcurrentRunner
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class MemberBlockTest : MemberWebTest() {

    @BeforeEach
    fun createMembers() {
        insertMember(REQUESTER_ID)
        insertMember(TARGET_ID)
        insertMember(OTHER_ID)
    }

    @Test
    @DisplayName("존재하지 않는 회원을 차단하면 404 NOT_FOUND_MEMBER로 응답하고 차단 기록을 남기지 않는다")
    fun blockMissingMember() {
        // given
        val missingId = 987_654L

        // when
        val result = mockMvc.perform(blockRequest(REQUESTER_ID, missingId))

        // then
        result.andExpect(status().isNotFound)
            .andExpect(jsonPath("$.errorCode").value("NOT_FOUND_MEMBER"))
        assertThat(count("member_block_relation", "member_id = ? OR blocked_member_id = ?", missingId, missingId))
            .isEqualTo(0)
    }

    @Test
    @DisplayName("자기 자신을 차단하면 400 CANNOT_BLOCK_SELF로 응답하고 차단 기록을 남기지 않는다")
    fun blockSelf() {
        // given

        // when
        val result = mockMvc.perform(blockRequest(REQUESTER_ID, REQUESTER_ID))

        // then
        result.andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errorCode").value("CANNOT_BLOCK_SELF"))
        assertThat(count("member_block_relation", "member_id = ?", REQUESTER_ID)).isEqualTo(0)
    }

    @Test
    @DisplayName("회원을 차단하면 양쪽 관계를 기록하고 서로의 큐에서 상대 사진만 지운다")
    fun blockRemovesQueuesOfBothSides() {
        // given
        val requesterPhoto = insertBodyPhoto(REQUESTER_ID)
        val targetPhoto = insertBodyPhoto(TARGET_ID)
        val otherPhoto = insertBodyPhoto(OTHER_ID)
        insertQueue(REQUESTER_ID, targetPhoto, TARGET_ID)
        insertQueue(REQUESTER_ID, otherPhoto, OTHER_ID)
        insertQueue(TARGET_ID, requesterPhoto, REQUESTER_ID)

        // when
        val result = mockMvc.perform(blockRequest(REQUESTER_ID, TARGET_ID))
        awaitAsyncIdle()

        // then
        result.andExpect(status().isOk)
        assertThat(count("member_block_relation", "member_id = ? AND blocked_member_id = ?", REQUESTER_ID, TARGET_ID))
            .isEqualTo(1)
        assertThat(count("member_block_relation", "member_id = ? AND blocked_member_id = ?", TARGET_ID, REQUESTER_ID))
            .isEqualTo(1)
        assertThat(count("reviewable_body_photo", "member_id = ? AND body_photo_member_id = ?", REQUESTER_ID, TARGET_ID))
            .isEqualTo(0)
        assertThat(count("reviewable_body_photo", "member_id = ? AND body_photo_member_id = ?", TARGET_ID, REQUESTER_ID))
            .isEqualTo(0)
        assertThat(count("reviewable_body_photo", "member_id = ? AND body_photo_id = ?", REQUESTER_ID, otherPhoto))
            .isEqualTo(1)
    }

    @Test
    @DisplayName("차단 요청자의 큐 잠금이 잡혀 있으면 잠금이 풀릴 때까지 차단이 끝나지 않는다")
    fun blockWaitsForRequesterQueueLock() {
        assertBlockWaitsForLockOf(REQUESTER_ID)
    }

    @Test
    @DisplayName("차단 대상의 큐 잠금이 잡혀 있으면 잠금이 풀릴 때까지 차단이 끝나지 않는다")
    fun blockWaitsForTargetQueueLock() {
        assertBlockWaitsForLockOf(TARGET_ID)
    }

    @Test
    @DisplayName("두 회원이 서로를 동시에 차단해도 교착 없이 둘 다 성공한다")
    fun mutualBlockDoesNotDeadlock() {
        repeat(ROUNDS) {
            // given
            jdbcTemplate.update("DELETE FROM member_block_relation")

            // when
            val statuses = ConcurrentRunner.runAll(
                listOf(
                    { mockMvc.perform(blockRequest(REQUESTER_ID, TARGET_ID)).andReturn().response.status },
                    { mockMvc.perform(blockRequest(TARGET_ID, REQUESTER_ID)).andReturn().response.status },
                ),
            ).map { it.getOrThrow() }
            awaitAsyncIdle()

            // then
            assertThat(statuses).containsOnly(200)
            assertThat(count("member_block_relation", "member_id IN (?, ?)", REQUESTER_ID, TARGET_ID)).isEqualTo(2)
        }
    }

    private fun assertBlockWaitsForLockOf(lockedMemberId: Long) {
        // given
        var statusWhileLocked: CompletableFuture<Int>? = null

        // when
        whileHoldingQueueLock(lockedMemberId) {
            statusWhileLocked = CompletableFuture.supplyAsync {
                mockMvc.perform(blockRequest(REQUESTER_ID, TARGET_ID)).andReturn().response.status
            }
            Thread.sleep(BLOCKED_WAIT_MILLIS)

            // then
            assertThat(statusWhileLocked!!.isDone).isFalse()
        }
        assertThat(statusWhileLocked!!.get(15, TimeUnit.SECONDS)).isEqualTo(200)
        awaitAsyncIdle()
    }

    companion object {
        private const val REQUESTER_ID = 900_601L
        private const val TARGET_ID = 900_602L
        private const val OTHER_ID = 900_603L
        private const val ROUNDS = 10
    }

}
