package app.thunder.api.member

import app.thunder.api.ThunderApiWebTest
import org.assertj.core.api.Assertions.assertThat
import org.instancio.Instancio
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class MemberNicknameTest : ThunderApiWebTest() {

    @Test
    @DisplayName("아무도 쓰지 않는 닉네임이면 인증 없이도 사용 가능하다고 응답한다")
    fun availableNickname() {
        // given
        val nickname = Instancio.gen().string().alphaNumeric().length(NICKNAME_LENGTH).get()

        // when
        val result = mockMvc.perform(get("/v1/member/nickname/available").param("nickname", nickname))

        // then
        result.andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("The nickname is available."))
            .andExpect(jsonPath("$.path").value("/v1/member/nickname/available"))
    }

    @Test
    @DisplayName("이미 쓰는 닉네임이면 409 NICKNAME_DUPLICATED로 응답한다")
    fun duplicatedNickname() {
        // given
        insertMember(MEMBER_ID)

        // when
        val result = mockMvc.perform(get("/v1/member/nickname/available").param("nickname", "user$MEMBER_ID"))

        // then
        result.andExpect(status().isConflict)
            .andExpect(jsonPath("$.errorCode").value("NICKNAME_DUPLICATED"))
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM member", Int::class.java)).isEqualTo(1)
    }

    @Test
    @DisplayName("닉네임 파라미터가 없으면 400 INVALID_PARAMETER_VALUE로 응답하고 빠진 파라미터 이름을 알려준다")
    fun missingNickname() {
        // given
        val path = "/v1/member/nickname/available"

        // when
        val result = mockMvc.perform(get(path))

        // then
        result.andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errorCode").value("INVALID_PARAMETER_VALUE"))
            .andExpect(jsonPath("$.message").value("Required parameter 'nickname' is not present."))
            .andExpect(jsonPath("$.path").value(path))
    }

    @Test
    @DisplayName("닉네임이 공백이면 400 INVALID_PARAMETER_VALUE로 응답한다")
    fun blankNickname() {
        // given
        val blank = "   "

        // when
        val result = mockMvc.perform(get("/v1/member/nickname/available").param("nickname", blank))

        // then
        result.andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errorCode").value("INVALID_PARAMETER_VALUE"))
    }

    companion object {
        private const val MEMBER_ID = 900_911L
        private const val NICKNAME_LENGTH = 8
    }

}
