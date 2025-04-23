package app.thunder.api.application

import app.thunder.api.ThunderApiWebTest
import java.time.LocalDateTime
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class AuthSignupConflictTest : ThunderApiWebTest() {

    @Test
    @DisplayName("이미 가입된 휴대폰 번호로 가입하면 409 MOBILE_NUMBER_DUPLICATED로 응답하고 회원이 늘지 않는다")
    fun duplicatedMobileNumber() {
        // given
        insertMember(EXISTING_MEMBER_ID)
        insertVerification("device-1", EXISTING_MOBILE_NUMBER, verifiedAt = LocalDateTime.now())
        val request = signupRequest(nickname = "newbie1", mobileNumber = EXISTING_MOBILE_NUMBER)

        // when
        val result = mockMvc.perform(request)

        // then
        result.andExpect(status().isConflict)
            .andExpect(jsonPath("$.errorCode").value("MOBILE_NUMBER_DUPLICATED"))
        assertThat(memberCount()).isEqualTo(1)
    }

    @Test
    @DisplayName("이미 쓰는 닉네임으로 가입하면 409 NICKNAME_DUPLICATED로 응답하고 회원이 늘지 않는다")
    fun duplicatedNickname() {
        // given
        insertMember(EXISTING_MEMBER_ID)
        jdbcTemplate.update("UPDATE member SET nickname = ? WHERE member_id = ?", TAKEN_NICKNAME, EXISTING_MEMBER_ID)
        insertVerification("device-1", NEW_MOBILE_NUMBER, verifiedAt = LocalDateTime.now())
        val request = signupRequest(nickname = TAKEN_NICKNAME, mobileNumber = NEW_MOBILE_NUMBER)

        // when
        val result = mockMvc.perform(request)

        // then
        result.andExpect(status().isConflict)
            .andExpect(jsonPath("$.errorCode").value("NICKNAME_DUPLICATED"))
        assertThat(memberCount()).isEqualTo(1)
    }

    private fun memberCount(): Int =
        jdbcTemplate.queryForObject("SELECT COUNT(*) FROM member", Int::class.java)!!

    private fun signupRequest(nickname: String, mobileNumber: String) =
        post("/v1/member/signup").contentType(APPLICATION_JSON).content(
            """
            {"nickname":"$nickname","mobileCountry":"KR","mobileNumber":"$mobileNumber","gender":"MALE",
             "birthDay":"1990-01-01","countryCode":"KR","marketingAgreement":false}
            """.trimIndent(),
        )

    companion object {
        private const val EXISTING_MEMBER_ID = 1_234_567_890L
        private const val EXISTING_MOBILE_NUMBER = "0101234567890"
        private const val NEW_MOBILE_NUMBER = "01099998888"
        private const val TAKEN_NICKNAME = "taken1"
    }

}
