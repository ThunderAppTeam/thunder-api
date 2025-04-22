package app.thunder.infrastructure.postgresql.review

import app.thunder.infrastructure.postgresql.PostgresqlJpaTest
import java.time.LocalDateTime
import java.util.UUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate

@PostgresqlJpaTest
class DummyDeckRepositoryImplTest {

    @Autowired
    private lateinit var repository: DummyDeckRepositoryImpl

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    private var requesterId = 0L

    @BeforeEach
    fun createDummyPhotos() {
        val maleDummyId = insertMember("dummy1")
        val femaleDummyId = insertMember("dummy2")
        repeat(PHOTOS_PER_DUMMY) {
            insertPhoto(maleDummyId)
            insertPhoto(femaleDummyId)
        }
        requesterId = insertMember("requester")
        repository.setUp()
    }

    @Test
    @DisplayName("처음 만든 더미 덱은 사진마다 한 행씩이고 dummyDeckId가 실제 저장된 값이다")
    fun firstGenerationReturnsStoredIds() {
        // given
        val expectedSize = PHOTOS_PER_DUMMY * 2

        // when
        val deck = repository.getAllByMemberId(requesterId)

        // then
        assertThat(deck).hasSize(expectedSize)
        assertThat(deck.map { it.dummyDeckId }).allMatch { it > 0 }.doesNotHaveDuplicates()
        assertThat(deck.map { it.dummyDeckId })
            .containsExactlyInAnyOrderElementsOf(storedIds(requesterId))
    }

    @Test
    @DisplayName("이미 만든 더미 덱을 다시 조회하면 같은 행을 돌려주고 새로 만들지 않는다")
    fun secondReadReturnsSameRows() {
        // given
        val first = repository.getAllByMemberId(requesterId)

        // when
        val second = repository.getAllByMemberId(requesterId)

        // then
        assertThat(second.map { it.dummyDeckId }).containsExactlyElementsOf(first.map { it.dummyDeckId })
        assertThat(storedIds(requesterId)).hasSize(PHOTOS_PER_DUMMY * 2)
    }

    @Test
    @DisplayName("회원의 더미 덱을 지우면 그 회원의 행만 사라진다")
    fun deleteAllByMemberId() {
        // given
        val otherRequesterId = insertMember("other-requester")
        repository.getAllByMemberId(requesterId)
        repository.getAllByMemberId(otherRequesterId)

        // when
        repository.deleteAllByMemberId(requesterId)

        // then
        assertThat(storedIds(requesterId)).isEmpty()
        assertThat(storedIds(otherRequesterId)).hasSize(PHOTOS_PER_DUMMY * 2)
    }

    @Test
    @DisplayName("exists는 회원의 더미 덱에 있는 사진만 true로 돌려주고 덱을 새로 만들지 않는다")
    fun existsChecksOnlyOwnDeckWithoutGenerating() {
        // given
        val otherRequesterId = insertMember("other-requester")
        val deck = repository.getAllByMemberId(requesterId)
        val bodyPhotoId = deck.first().bodyPhotoId

        // when
        val inOwnDeck = repository.exists(requesterId, bodyPhotoId)
        val inOtherDeck = repository.exists(otherRequesterId, bodyPhotoId)

        // then
        assertThat(inOwnDeck).isTrue()
        assertThat(inOtherDeck).isFalse()
        assertThat(storedIds(otherRequesterId)).isEmpty()
    }

    private fun storedIds(memberId: Long): List<Long> =
        jdbcTemplate.queryForList("SELECT dummy_deck_id FROM dummy_deck WHERE member_id = ?", Long::class.java, memberId)

    private fun insertMember(nickname: String): Long = jdbcTemplate.queryForObject(
        """
        INSERT INTO member (nickname, mobile_number, mobile_country, gender, birth_day, country_code,
                            marketing_agreement, member_uuid, created_at)
        VALUES (?, ?, 'KR', 'MALE', DATE '1990-01-01', 'KR', FALSE, ?, ?) RETURNING member_id
        """.trimIndent(),
        Long::class.java, nickname, "010${UUID.randomUUID().toString().take(8)}", UUID.randomUUID(), LocalDateTime.now(),
    )!!

    private fun insertPhoto(memberId: Long) {
        jdbcTemplate.update(
            """
            INSERT INTO body_photo (member_id, image_url, review_count, total_review_score, created_at)
            VALUES (?, ?, 0, 0, ?)
            """.trimIndent(),
            memberId, "https://bucket/${UUID.randomUUID()}.jpg", LocalDateTime.now(),
        )
    }

    companion object {
        private const val PHOTOS_PER_DUMMY = 3
    }

}
