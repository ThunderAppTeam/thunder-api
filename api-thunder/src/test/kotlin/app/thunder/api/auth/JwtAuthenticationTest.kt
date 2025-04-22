package app.thunder.api.auth

import app.thunder.api.ThunderApiWebTest
import java.time.Instant
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class JwtAuthenticationTest : ThunderApiWebTest() {

    @Test
    @DisplayName("올바르게 서명됐지만 subject가 숫자가 아닌 토큰은 401 INVALID_TOKEN으로 응답한다")
    fun nonNumericSubject() {
        // given
        val token = tokenManager.generateAccessToken("not-a-number")

        // when
        val result = mockMvc.perform(get("/v1/member/info").header(AUTHORIZATION, "Bearer $token"))

        // then
        result.andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.errorCode").value("INVALID_TOKEN"))
    }

    @Test
    @DisplayName("서명이 올바르지 않은 토큰은 401 INVALID_TOKEN으로 응답한다")
    fun invalidSignature() {
        // given
        val token = "invalid.token.value"

        // when
        val result = mockMvc.perform(get("/v1/member/info").header(AUTHORIZATION, "Bearer $token"))

        // then
        result.andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.errorCode").value("INVALID_TOKEN"))
    }

    @Test
    @DisplayName("Authorization 헤더 없이 인증이 필요한 API를 호출하면 INVALID_TOKEN 본문과 함께 401로 응답한다")
    fun missingAuthorizationHeader() {
        // given
        val path = "/v1/member/info"

        // when
        val result = mockMvc.perform(get(path))

        // then
        result.andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.errorCode").value("INVALID_TOKEN"))
            .andExpect(jsonPath("$.message").value("The token is invalid."))
            .andExpect(jsonPath("$.path").value(path))
    }

    @Test
    @DisplayName("인증이 필요 없는 health API는 토큰 없이 호출할 수 있다")
    fun publicEndpointStaysPublic() {
        // given
        val path = "/health"

        // when
        val result = mockMvc.perform(get(path))

        // then
        result.andExpect(status().isOk)
    }

    @Test
    @DisplayName("존재하지 않는 회원의 토큰은 401 INVALID_TOKEN으로 응답한다")
    fun tokenOfDeletedMember() {
        // given
        val authorization = bearerToken(DELETED_MEMBER_ID)

        // when
        val result = mockMvc.perform(get("/v1/member/info").header(AUTHORIZATION, authorization))

        // then
        result.andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.errorCode").value("INVALID_TOKEN"))
    }

    @Test
    @DisplayName("로그아웃 시각 이전에 발급된 토큰은 401 INVALID_TOKEN으로 응답한다")
    fun tokenIssuedBeforeLogout() {
        // given
        insertMember(LOGGED_OUT_MEMBER_ID, loggedOutAt = LocalDateTime.now().plusSeconds(10))
        val authorization = bearerToken(LOGGED_OUT_MEMBER_ID)

        // when
        val result = mockMvc.perform(get("/v1/member/info").header(AUTHORIZATION, authorization))

        // then
        result.andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.errorCode").value("INVALID_TOKEN"))
    }

    @Test
    @DisplayName("로그아웃 시각 이후에 발급된 토큰은 계속 사용할 수 있다")
    fun tokenIssuedAfterLogout() {
        // given
        insertMember(LOGGED_OUT_MEMBER_ID, loggedOutAt = LocalDateTime.now().minusSeconds(10))
        val authorization = bearerToken(LOGGED_OUT_MEMBER_ID)

        // when
        val result = mockMvc.perform(get("/v1/member/info").header(AUTHORIZATION, authorization))

        // then
        result.andExpect(status().isOk)
    }

    @Test
    @DisplayName("로그아웃한 회원이 같은 초에 다시 발급받은 토큰은 사용할 수 있다")
    fun tokenIssuedInSameSecondAsLogout() {
        // given
        insertMember(LOGGED_OUT_MEMBER_ID, loggedOutAt = LocalDateTime.now())
        val authorization = bearerToken(LOGGED_OUT_MEMBER_ID)

        // when
        val result = mockMvc.perform(get("/v1/member/info").header(AUTHORIZATION, authorization))

        // then
        result.andExpect(status().isOk)
    }

    @Test
    @DisplayName("토큰에서 발급 시각을 읽을 수 있다")
    fun issuedAtIsExposed() {
        // given
        val before = Instant.now().truncatedTo(ChronoUnit.SECONDS)
        val token = tokenManager.generateAccessToken(DELETED_MEMBER_ID)

        // when
        val claims = tokenManager.parse(token)

        // then
        assertThat(claims.subject).isEqualTo(DELETED_MEMBER_ID.toString())
        assertThat(claims.issuedAt).isBetween(before, Instant.now())
    }

    companion object {
        private const val DELETED_MEMBER_ID = 900_101L
        private const val LOGGED_OUT_MEMBER_ID = 900_102L
    }


}
