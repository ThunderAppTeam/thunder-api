package app.thunder.api.auth

import app.thunder.api.ThunderApiWebTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class AdminAuthorizationTest : ThunderApiWebTest() {

    @BeforeEach
    fun createMembers() {
        insertMember(ADMIN_MEMBER_ID)
        insertMember(NORMAL_MEMBER_ID)
    }

    @Test
    @DisplayName("토큰 없이 release-ui를 조회해도 200으로 응답한다")
    fun releaseUiQueryIsPublic() {
        // given
        val path = "/v1/admin/release-ui?mobileOs=IOS&appVersion=1.0.0"

        // when
        val result = mockMvc.perform(get(path))

        // then
        result.andExpect(status().isOk)
    }

    @Test
    @DisplayName("관리자가 아닌 회원이 HEAD로 release-ui를 호출하면 403 ADMIN_ONLY_ACCESS로 응답한다")
    fun nonAdminCannotCallHeadOnReleaseUi() {
        // given
        val authorization = bearerToken(NORMAL_MEMBER_ID)

        // when
        val result = mockMvc.perform(head("/v1/admin/release-ui").header(AUTHORIZATION, authorization))

        // then
        result.andExpect(status().isForbidden)
    }

    @Test
    @DisplayName("관리자가 아닌 회원이 release-ui를 변경하면 403 ADMIN_ONLY_ACCESS로 응답한다")
    fun nonAdminCannotChangeReleaseUi() {
        // given
        val authorization = bearerToken(NORMAL_MEMBER_ID)

        // when
        val result = mockMvc.perform(
            post("/v1/admin/release-ui").header(AUTHORIZATION, authorization)
                .contentType(APPLICATION_JSON).content(RELEASE_UI_BODY),
        )

        // then
        result.andExpect(status().isForbidden)
            .andExpect(jsonPath("$.errorCode").value("ADMIN_ONLY_ACCESS"))
            .andExpect(jsonPath("$.message").value("Only admin has permission for this request."))
            .andExpect(jsonPath("$.path").value("/v1/admin/release-ui"))
    }

    @Test
    @DisplayName("관리자 회원은 release-ui를 변경할 수 있다")
    fun adminCanChangeReleaseUi() {
        // given
        val authorization = bearerToken(ADMIN_MEMBER_ID)

        // when
        val result = mockMvc.perform(
            post("/v1/admin/release-ui").header(AUTHORIZATION, authorization)
                .contentType(APPLICATION_JSON).content(RELEASE_UI_BODY),
        )

        // then
        result.andExpect(status().isOk)
    }

    @Test
    @DisplayName("인증 없이 release-ui를 변경하면 401로 응답한다")
    fun anonymousCannotChangeReleaseUi() {
        // given
        val body = RELEASE_UI_BODY

        // when
        val result = mockMvc.perform(post("/v1/admin/release-ui").contentType(APPLICATION_JSON).content(body))

        // then
        result.andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.errorCode").value("INVALID_TOKEN"))
    }

    @Test
    @DisplayName("관리자가 아닌 회원이 SMS 발송 제한을 초기화하면 403 ADMIN_ONLY_ACCESS로 응답한다")
    fun nonAdminCannotResetSendLimit() {
        // given
        val authorization = bearerToken(NORMAL_MEMBER_ID)

        // when
        val result = mockMvc.perform(
            post("/v1/member/sms/reset").header(AUTHORIZATION, authorization)
                .contentType(APPLICATION_JSON).content("""{"deviceId":"device-1"}"""),
        )

        // then
        result.andExpect(status().isForbidden)
            .andExpect(jsonPath("$.errorCode").value("ADMIN_ONLY_ACCESS"))
    }

    @Test
    @DisplayName("관리자 회원은 SMS 발송 제한을 초기화할 수 있다")
    fun adminCanResetSendLimit() {
        // given
        val authorization = bearerToken(ADMIN_MEMBER_ID)

        // when
        val result = mockMvc.perform(
            post("/v1/member/sms/reset").header(AUTHORIZATION, authorization)
                .contentType(APPLICATION_JSON).content("""{"deviceId":"device-1"}"""),
        )

        // then
        result.andExpect(status().isOk)
    }

    @Test
    @DisplayName("인증 없이 SMS 발송 제한을 초기화하면 401로 응답한다")
    fun anonymousCannotResetSendLimit() {
        // given
        val body = """{"deviceId":"device-1"}"""

        // when
        val result = mockMvc.perform(post("/v1/member/sms/reset").contentType(APPLICATION_JSON).content(body))

        // then
        result.andExpect(status().isUnauthorized)
    }

    companion object {
        private const val NORMAL_MEMBER_ID = 900_002L
        private const val RELEASE_UI_BODY = """{"mobileOs":"IOS","appVersion":"1.0.0","isRelease":true}"""
    }

}
