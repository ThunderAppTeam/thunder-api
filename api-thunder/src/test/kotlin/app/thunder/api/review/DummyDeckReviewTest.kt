package app.thunder.api.review

import java.time.LocalDateTime
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class DummyDeckReviewTest : BodyReviewWebTest() {

    @BeforeEach
    fun createMembers() {
        insertMember(OWNER_ID)
        insertMember(REVIEWER_ID)
    }

    @Test
    @DisplayName("더미 덱에 있는 사진을 리뷰하면 더미 행만 지우고 리뷰나 점수는 기록하지 않는다")
    fun reviewDummyDeckPhoto() {
        // given
        val bodyPhotoId = insertBodyPhoto(OWNER_ID)
        insertDummyDeckRow(REVIEWER_ID, bodyPhotoId, OWNER_ID)

        // when
        val result = mockMvc.perform(reviewRequest(REVIEWER_ID, bodyPhotoId, score = 5))

        // then
        result.andExpect(status().isOk)
        assertThat(count("dummy_deck", "member_id = ? AND body_photo_id = ?", REVIEWER_ID, bodyPhotoId)).isEqualTo(0)
        assertThat(count("body_review", "body_photo_id = ?", bodyPhotoId)).isEqualTo(0)
        assertThat(reviewCount(bodyPhotoId)).isEqualTo(0)
        assertThat(totalReviewScore(bodyPhotoId)).isEqualTo(0.0)
    }

    private fun insertDummyDeckRow(memberId: Long, bodyPhotoId: Long, bodyPhotoMemberId: Long) {
        jdbcTemplate.update(
            """
            INSERT INTO dummy_deck (member_id, body_photo_id, body_photo_member_id, nickname, age, created_at)
            VALUES (?, ?, ?, 'dummy', 25, ?)
            """.trimIndent(),
            memberId, bodyPhotoId, bodyPhotoMemberId, LocalDateTime.now(),
        )
    }

    companion object {
        private const val OWNER_ID = 900_421L
        private const val REVIEWER_ID = 900_422L
    }

}
