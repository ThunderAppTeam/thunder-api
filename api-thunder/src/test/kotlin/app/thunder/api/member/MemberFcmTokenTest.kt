package app.thunder.api.member

import app.thunder.api.ConcurrentRunner
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class MemberFcmTokenTest : MemberWebTest() {

    @BeforeEach
    fun createMembers() {
        insertMember(FIRST_ID)
        insertMember(SECOND_ID)
    }

    @Test
    @DisplayName("로그아웃하면 그 회원의 FCM 토큰을 지우고 다른 회원의 토큰은 남긴다")
    fun logoutDeletesToken() {
        // given
        insertToken(FIRST_ID, "first-token")
        insertToken(SECOND_ID, "second-token")

        // when
        val result = mockMvc.perform(logoutRequest(FIRST_ID))

        // then
        result.andExpect(status().isOk)
        assertThat(tokenOf(FIRST_ID)).isNull()
        assertThat(tokenOf(SECOND_ID)).isEqualTo("second-token")
    }

    @Test
    @DisplayName("다른 회원이 쓰던 FCM 토큰을 저장하면 이전 회원의 토큰은 사라진다")
    fun saveTokenMovesItFromOtherMember() {
        // given
        insertToken(FIRST_ID, SHARED_TOKEN)

        // when
        val result = mockMvc.perform(fcmTokenRequest(SECOND_ID, SHARED_TOKEN))

        // then
        result.andExpect(status().isOk)
        assertThat(tokenOf(FIRST_ID)).isNull()
        assertThat(tokenOf(SECOND_ID)).isEqualTo(SHARED_TOKEN)
    }

    @Test
    @DisplayName("두 회원이 같은 FCM 토큰을 동시에 저장하면 한 회원만 토큰을 가지고 서버 오류는 없다")
    fun concurrentSaveOfSameToken() {
        repeat(ROUNDS) {
            // given
            jdbcTemplate.update("DELETE FROM member_fcm_token")

            // when
            val statuses = ConcurrentRunner.runAll(
                listOf(
                    { mockMvc.perform(fcmTokenRequest(FIRST_ID, SHARED_TOKEN)).andReturn().response.status },
                    { mockMvc.perform(fcmTokenRequest(SECOND_ID, SHARED_TOKEN)).andReturn().response.status },
                ),
            ).map { it.getOrThrow() }

            // then
            assertThat(statuses).containsAnyOf(200).isSubsetOf(200, 409)
            assertThat(count("member_fcm_token", "fcm_token = ?", SHARED_TOKEN)).isEqualTo(1)
        }
    }

    companion object {
        private const val FIRST_ID = 900_801L
        private const val SECOND_ID = 900_802L
        private const val SHARED_TOKEN = "shared-device-token"
        private const val ROUNDS = 10
    }

}
