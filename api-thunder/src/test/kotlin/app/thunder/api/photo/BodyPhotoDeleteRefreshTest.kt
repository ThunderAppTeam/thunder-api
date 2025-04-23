package app.thunder.api.photo

import app.thunder.api.review.BodyReviewWebTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class BodyPhotoDeleteRefreshTest : BodyReviewWebTest() {

    @BeforeEach
    fun createMembers() {
        insertMember(OWNER_ID)
        insertMember(REVIEWER_ID)
        insertMember(OTHER_OWNER_ID)
    }

    @Test
    @DisplayName("사진을 지우면 그 사진을 큐에 가진 회원의 큐를 다시 채워 다른 사진으로 교체한다")
    fun deleteRefillsQueuesOfMembersHoldingThePhoto() {
        // given
        val deleted = insertBodyPhoto(OWNER_ID)
        val replacement = insertBodyPhoto(OTHER_OWNER_ID)
        insertQueue(REVIEWER_ID, deleted, OWNER_ID)

        // when
        val result = mockMvc.perform(
            delete("/v1/body/photo/$deleted").header(AUTHORIZATION, bearerToken(OWNER_ID)),
        )
        awaitAsyncIdle()

        // then
        result.andExpect(status().isOk)
        assertThat(count("reviewable_body_photo", "body_photo_id = ?", deleted)).isEqualTo(0)
        assertThat(count("reviewable_body_photo", "member_id = ? AND body_photo_id = ?", REVIEWER_ID, replacement))
            .isEqualTo(1)
    }

    companion object {
        private const val OWNER_ID = 900_341L
        private const val REVIEWER_ID = 900_342L
        private const val OTHER_OWNER_ID = 900_343L
    }

}
