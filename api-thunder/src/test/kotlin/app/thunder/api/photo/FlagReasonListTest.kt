package app.thunder.api.photo

import app.thunder.core.flag.domain.FlagReason
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class FlagReasonListTest : BodyPhotoWebTest() {

    @BeforeEach
    fun createMember() {
        insertMember(MEMBER_ID)
    }

    @Test
    @DisplayName("국가 코드 없이 신고 사유를 조회하면 모든 신고 사유를 한국어 설명과 함께 정의된 순서대로 응답한다")
    fun listReasonsWithDefaultCountry() {
        // given
        val reasons = FlagReason.entries

        // when
        val result = mockMvc.perform(get("/v1/body/flag").header(AUTHORIZATION, bearerToken(MEMBER_ID)))

        // then
        result.andExpect(status().isOk)
            .andExpect(jsonPath("$.data.length()").value(reasons.size))
        reasons.forEachIndexed { index, reason ->
            result.andExpect(jsonPath("$.data[$index].flagReason").value(reason.name))
                .andExpect(jsonPath("$.data[$index].description").value(reason.descriptionKR))
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["KR", "US"])
    @DisplayName("국가 코드를 지정해 신고 사유를 조회해도 한국어 설명으로 응답한다")
    fun listReasonsWithCountry(countryCode: String) {
        // given
        val firstReason = FlagReason.entries.first()

        // when
        val result = mockMvc.perform(
            get("/v1/body/flag").param("countryCode", countryCode).header(AUTHORIZATION, bearerToken(MEMBER_ID)),
        )

        // then
        result.andExpect(status().isOk)
            .andExpect(jsonPath("$.data.length()").value(FlagReason.entries.size))
            .andExpect(jsonPath("$.data[0].flagReason").value(firstReason.name))
            .andExpect(jsonPath("$.data[0].description").value(firstReason.descriptionKR))
    }

    companion object {
        private const val MEMBER_ID = 900_351L
    }

}
