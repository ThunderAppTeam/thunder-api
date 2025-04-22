package app.thunder.api.review

import app.thunder.api.ConcurrentRunner
import app.thunder.core.review.repository.DummyDeckRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.test.util.AopTestUtils
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get

class DummyDeckConcurrencyTest : BodyReviewWebTest() {

    @Autowired
    private lateinit var dummyDeckRepository: DummyDeckRepository

    @BeforeEach
    fun createDummyPhotos() {
        insertMember(MALE_DUMMY_ID)
        insertMember(FEMALE_DUMMY_ID)
        jdbcTemplate.update("UPDATE member SET nickname = 'dummy1' WHERE member_id = ?", MALE_DUMMY_ID)
        jdbcTemplate.update("UPDATE member SET nickname = 'dummy2' WHERE member_id = ?", FEMALE_DUMMY_ID)
        repeat(PHOTOS_PER_DUMMY) {
            keepOutOfQueues(insertBodyPhoto(MALE_DUMMY_ID))
            keepOutOfQueues(insertBodyPhoto(FEMALE_DUMMY_ID))
        }
        insertMember(REQUESTER_ID)
        reloadDummyPhotos()
    }

    @AfterEach
    fun forgetDummyPhotos() {
        awaitAsyncIdle()
        jdbcTemplate.update("DELETE FROM body_photo")
        jdbcTemplate.update("DELETE FROM member")
        reloadDummyPhotos()
    }

    @Test
    @DisplayName("큐가 빈 회원이 동시에 여러 번 조회해도 더미 덱은 사진마다 한 번만 만들어진다")
    fun firstGenerationHappensOnce() {
        // given
        val expectedSize = PHOTOS_PER_DUMMY * 2

        // when
        val responses = ConcurrentRunner.runAll(
            List(4) {
                { mockMvc.perform(get("/v1/body/review").param("size", "30").header(AUTHORIZATION, bearerToken(REQUESTER_ID))).andReturn().response }
            },
        ).map { it.getOrThrow() }

        // then
        assertThat(responses.map { it.status }).containsOnly(200)
        assertThat(count("dummy_deck", "member_id = ?", REQUESTER_ID)).isEqualTo(expectedSize)
        assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT body_photo_id) FROM dummy_deck WHERE member_id = ?", Int::class.java, REQUESTER_ID,
            ),
        ).isEqualTo(expectedSize)
    }

    private fun keepOutOfQueues(bodyPhotoId: Long) {
        (1..FLAG_LIMIT).forEach { insertFlagHistory(bodyPhotoId, FLAGGER_BASE_ID + it) }
    }

    private fun reloadDummyPhotos() {
        ReflectionTestUtils.invokeMethod<Any>(AopTestUtils.getUltimateTargetObject<Any>(dummyDeckRepository), "setUp")
    }

    companion object {
        private const val MALE_DUMMY_ID = 900_501L
        private const val FEMALE_DUMMY_ID = 900_502L
        private const val REQUESTER_ID = 900_503L
        private const val FLAGGER_BASE_ID = 900_510L
        private const val FLAG_LIMIT = 3
        private const val PHOTOS_PER_DUMMY = 3
    }

}
