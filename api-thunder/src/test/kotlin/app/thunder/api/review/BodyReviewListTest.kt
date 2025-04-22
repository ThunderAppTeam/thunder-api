package app.thunder.api.review

import java.time.LocalDateTime
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class BodyReviewListTest : BodyReviewWebTest() {

    @BeforeEach
    fun createMembers() {
        insertMember(OWNER_ID)
        insertMember(REVIEWER_ID)
    }

    @Test
    @DisplayName("size가 0이면 400으로 응답한다")
    fun sizeZero() {
        // given
        val size = 0

        // when
        val result = mockMvc.perform(listRequest(size))

        // then
        result.andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errorCode").value("INVALID_PARAMETER_VALUE"))
    }

    @Test
    @DisplayName("size가 31이면 400으로 응답한다")
    fun sizeAboveMaximum() {
        // given
        val size = 31

        // when
        val result = mockMvc.perform(listRequest(size))

        // then
        result.andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errorCode").value("INVALID_PARAMETER_VALUE"))
    }

    @Test
    @DisplayName("size가 1이면 큐의 첫 사진 하나만 응답한다")
    fun sizeOne() {
        // given
        repeat(3) { insertQueue(REVIEWER_ID, insertBodyPhoto(OWNER_ID), OWNER_ID) }

        // when
        val result = mockMvc.perform(listRequest(1))

        // then
        result.andExpect(status().isOk)
            .andExpect(jsonPath("$.data.length()").value(1))
    }

    @Test
    @DisplayName("size가 30이면 허용되어 큐의 사진을 모두 응답한다")
    fun sizeMaximum() {
        // given
        repeat(3) { insertQueue(REVIEWER_ID, insertBodyPhoto(OWNER_ID), OWNER_ID) }

        // when
        val result = mockMvc.perform(listRequest(30))

        // then
        result.andExpect(status().isOk)
            .andExpect(jsonPath("$.data.length()").value(3))
    }

    @Test
    @DisplayName("리뷰가 이미 끝난 사진은 큐에 남아 있어도 응답에서 건너뛰고 요청한 개수를 채운다")
    fun skipsCompletedPhotos() {
        // given
        val completed = insertBodyPhoto(OWNER_ID, reviewCount = 10, totalReviewScore = 40.0)
        val reviewable = insertBodyPhoto(OWNER_ID, reviewCount = 3, totalReviewScore = 12.0)
        val other = insertBodyPhoto(OWNER_ID)
        insertQueue(REVIEWER_ID, completed, OWNER_ID, LocalDateTime.now().minusMinutes(3))
        insertQueue(REVIEWER_ID, reviewable, OWNER_ID, LocalDateTime.now().minusMinutes(2))
        insertQueue(REVIEWER_ID, other, OWNER_ID, LocalDateTime.now().minusMinutes(1))

        // when
        val result = mockMvc.perform(listRequest(2))

        // then
        result.andExpect(status().isOk)
            .andExpect(jsonPath("$.data.length()").value(2))
            .andExpect(jsonPath("$.data[0].bodyPhotoId").value(reviewable))
            .andExpect(jsonPath("$.data[1].bodyPhotoId").value(other))
    }

    private fun listRequest(size: Int) =
        get("/v1/body/review").param("size", size.toString()).header(AUTHORIZATION, bearerToken(REVIEWER_ID))

    companion object {
        private const val OWNER_ID = 900_301L
        private const val REVIEWER_ID = 900_302L
    }

}
