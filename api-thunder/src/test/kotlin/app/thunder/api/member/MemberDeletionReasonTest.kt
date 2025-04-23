package app.thunder.api.member

import app.thunder.api.ThunderApiWebTest
import app.thunder.core.member.domain.MemberDeletionReason
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class MemberDeletionReasonTest : ThunderApiWebTest() {

    @Test
    @DisplayName("탈퇴 사유를 조회하면 모든 사유를 이름과 한국어 설명으로 돌려준다")
    fun defaultCountryIsKorea() {
        // given
        insertMember(MEMBER_ID)
        val authorization = bearerToken(MEMBER_ID)

        // when
        val result = mockMvc.perform(get("/v1/member/deletion-reason").header(AUTHORIZATION, authorization))

        // then
        result.andExpect(status().isOk)
            .andExpect(jsonPath("$.data.length()").value(MemberDeletionReason.entries.size))
            .andExpect(jsonPath("$.data[0].memberDeletionReason").value("DECREASED_INTEREST"))
            .andExpect(jsonPath("$.data[0].description").value("서비스에 대한 관심이 줄어듬"))
            .andExpect(jsonPath("$.data[7].memberDeletionReason").value("OTHER"))
            .andExpect(jsonPath("$.data[7].description").value("기타"))
    }

    @Test
    @DisplayName("지원하지 않는 국가 코드로 탈퇴 사유를 조회하면 한국어 설명으로 돌려준다")
    fun unsupportedCountryFallsBackToKorean() {
        // given
        insertMember(MEMBER_ID)
        val authorization = bearerToken(MEMBER_ID)

        // when
        val result = mockMvc.perform(
            get("/v1/member/deletion-reason").param("countryCode", "US").header(AUTHORIZATION, authorization),
        )

        // then
        result.andExpect(status().isOk)
            .andExpect(jsonPath("$.data.length()").value(MemberDeletionReason.entries.size))
            .andExpect(jsonPath("$.data[0].memberDeletionReason").value("DECREASED_INTEREST"))
            .andExpect(jsonPath("$.data[0].description").value("서비스에 대한 관심이 줄어듬"))
    }

    companion object {
        private const val MEMBER_ID = 900_921L
    }

}
