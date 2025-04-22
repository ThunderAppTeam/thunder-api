package app.thunder.api.photo

import app.thunder.api.review.BodyReviewWebTest
import java.time.LocalDateTime
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class FlagBodyPhotoTest : BodyReviewWebTest() {

    @BeforeEach
    fun createMembers() {
        insertMember(OWNER_ID)
        insertMember(FLAGGER_ID)
    }

    @Test
    @DisplayName("존재하지 않는 사진을 신고하면 404 NOT_FOUND_BODY_PHOTO로 응답하고 기록하지 않는다")
    fun flagMissingPhoto() {
        // given
        val missingId = 987_654L

        // when
        val result = mockMvc.perform(flagRequest(FLAGGER_ID, missingId))

        // then
        result.andExpect(status().isNotFound)
            .andExpect(jsonPath("$.errorCode").value("NOT_FOUND_BODY_PHOTO"))
        assertThat(count("flag_history", "body_photo_id = ?", missingId)).isEqualTo(0)
    }

    @Test
    @DisplayName("자기 사진을 신고하면 400 CANNOT_FLAG_OWN_PHOTO로 응답하고 기록하지 않는다")
    fun flagOwnPhoto() {
        // given
        val bodyPhotoId = insertBodyPhoto(OWNER_ID)

        // when
        val result = mockMvc.perform(flagRequest(OWNER_ID, bodyPhotoId))

        // then
        result.andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errorCode").value("CANNOT_FLAG_OWN_PHOTO"))
        assertThat(count("flag_history", "body_photo_id = ?", bodyPhotoId)).isEqualTo(0)
    }

    @Test
    @DisplayName("다른 회원의 사진을 신고하면 기록하고 같은 사진을 다시 신고하면 409로 응답한다")
    fun flagOtherPhotoTwice() {
        // given
        val bodyPhotoId = insertBodyPhoto(OWNER_ID)
        insertQueue(FLAGGER_ID, bodyPhotoId, OWNER_ID)

        // when
        val first = mockMvc.perform(flagRequest(FLAGGER_ID, bodyPhotoId))
        val second = mockMvc.perform(flagRequest(FLAGGER_ID, bodyPhotoId))

        // then
        first.andExpect(status().isOk)
        second.andExpect(status().isConflict)
            .andExpect(jsonPath("$.errorCode").value("ALREADY_FLAGGED"))
        assertThat(count("flag_history", "body_photo_id = ?", bodyPhotoId)).isEqualTo(1)
    }

    @Test
    @DisplayName("큐에도 더미 덱에도 없는 사진을 신고하면 403 NOT_REVIEWABLE_BODY_PHOTO로 응답하고 기록하지 않는다")
    fun flagNeverAssignedPhoto() {
        // given
        val bodyPhotoId = insertBodyPhoto(OWNER_ID)

        // when
        val result = mockMvc.perform(flagRequest(FLAGGER_ID, bodyPhotoId))

        // then
        result.andExpect(status().isForbidden)
            .andExpect(jsonPath("$.errorCode").value("NOT_REVIEWABLE_BODY_PHOTO"))
        assertThat(count("flag_history", "body_photo_id = ?", bodyPhotoId)).isEqualTo(0)
    }

    @Test
    @DisplayName("큐에 있는 사진을 신고하면 기록하고 큐에서 뺀다")
    fun flagQueuedPhoto() {
        // given
        val bodyPhotoId = insertBodyPhoto(OWNER_ID)
        insertQueue(FLAGGER_ID, bodyPhotoId, OWNER_ID)

        // when
        val result = mockMvc.perform(flagRequest(FLAGGER_ID, bodyPhotoId))
        awaitAsyncIdle()

        // then
        result.andExpect(status().isOk)
        assertThat(count("flag_history", "body_photo_id = ? AND member_id = ?", bodyPhotoId, FLAGGER_ID)).isEqualTo(1)
        assertThat(count("reviewable_body_photo", "member_id = ? AND body_photo_id = ?", FLAGGER_ID, bodyPhotoId))
            .isEqualTo(0)
    }

    @Test
    @DisplayName("더미 덱에 있는 사진을 신고하면 기록한다")
    fun flagDummyDeckPhoto() {
        // given
        val bodyPhotoId = insertBodyPhoto(OWNER_ID)
        insertDummyDeckRow(FLAGGER_ID, bodyPhotoId, OWNER_ID)

        // when
        val result = mockMvc.perform(flagRequest(FLAGGER_ID, bodyPhotoId))
        awaitAsyncIdle()

        // then
        result.andExpect(status().isOk)
        assertThat(count("flag_history", "body_photo_id = ? AND member_id = ?", bodyPhotoId, FLAGGER_ID)).isEqualTo(1)
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

    private fun flagRequest(memberId: Long, bodyPhotoId: Long) =
        post("/v1/body/flag")
            .header(AUTHORIZATION, bearerToken(memberId))
            .contentType(APPLICATION_JSON)
            .content("""{"bodyPhotoId":$bodyPhotoId,"flagReason":"OTHER","otherReason":"test"}""")

    companion object {
        private const val OWNER_ID = 900_041L
        private const val FLAGGER_ID = 900_042L
    }

}
