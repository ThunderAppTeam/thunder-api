package app.thunder.api.review

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class BodyReviewDefaultSizeTest : BodyReviewWebTest() {

    @BeforeEach
    fun createMembers() {
        insertMember(OWNER_ID)
        insertMember(REVIEWER_ID)
    }

    @Test
    @DisplayName("size를 지정하지 않으면 큐의 사진을 5개까지 응답한다")
    fun defaultSizeIsFive() {
        // given
        repeat(7) { insertQueue(REVIEWER_ID, insertBodyPhoto(OWNER_ID), OWNER_ID) }

        // when
        val result = mockMvc.perform(get("/v1/body/review").header(AUTHORIZATION, bearerToken(REVIEWER_ID)))

        // then
        result.andExpect(status().isOk)
            .andExpect(jsonPath("$.data.length()").value(5))
    }

    companion object {
        private const val OWNER_ID = 900_411L
        private const val REVIEWER_ID = 900_412L
    }

}
