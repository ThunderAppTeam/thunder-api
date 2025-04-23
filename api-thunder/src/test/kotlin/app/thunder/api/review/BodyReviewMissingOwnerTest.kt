package app.thunder.api.review

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class BodyReviewMissingOwnerTest : BodyReviewWebTest() {

    @BeforeEach
    fun createMembers() {
        insertMember(OWNER_ID)
        insertMember(REVIEWER_ID)
    }

    @Test
    @DisplayName("큐에 있는 사진의 주인이 이미 없으면 그 사진은 응답에서 빼고 나머지 사진만 응답한다")
    fun photoWithoutOwnerIsSkipped() {
        // given
        val orphanPhoto = insertBodyPhoto(DELETED_OWNER_ID)
        val validPhoto = insertBodyPhoto(OWNER_ID)
        insertQueue(REVIEWER_ID, orphanPhoto, DELETED_OWNER_ID)
        insertQueue(REVIEWER_ID, validPhoto, OWNER_ID)

        // when
        val result = mockMvc.perform(
            get("/v1/body/review").param("size", "5").header(AUTHORIZATION, bearerToken(REVIEWER_ID)),
        )

        // then
        result.andExpect(status().isOk)
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].bodyPhotoId").value(validPhoto))
            .andExpect(jsonPath("$.data[0].memberId").value(OWNER_ID))
    }

    companion object {
        private const val OWNER_ID = 900_441L
        private const val REVIEWER_ID = 900_442L
        private const val DELETED_OWNER_ID = 900_443L
    }

}
