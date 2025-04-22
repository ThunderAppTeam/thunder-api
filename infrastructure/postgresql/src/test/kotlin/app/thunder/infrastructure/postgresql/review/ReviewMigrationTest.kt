package app.thunder.infrastructure.postgresql.review

import app.thunder.infrastructure.postgresql.PostgresqlJpaTest
import java.time.LocalDateTime
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ClassPathResource
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.jdbc.core.JdbcTemplate

@PostgresqlJpaTest
class ReviewMigrationTest {

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Test
    @DisplayName("리뷰 중복 제거 migration은 회원과 사진마다 가장 이른 리뷰만 남기고 사진 카운터는 건드리지 않는다")
    fun dedupeBodyReviews() {
        // given
        jdbcTemplate.execute("DROP INDEX IF EXISTS $BODY_REVIEW_UNIQUE_INDEX")
        val base = LocalDateTime.of(2026, 1, 1, 0, 0)
        val photoId = insertBodyPhoto(reviewCount = 3, totalReviewScore = 12.0)
        val earliest = insertBodyReview(photoId, MEMBER_ID, base.plusMinutes(1))
        insertBodyReview(photoId, MEMBER_ID, base.plusMinutes(2))
        val tieFirst = insertBodyReview(photoId, OTHER_MEMBER_ID, base)
        insertBodyReview(photoId, OTHER_MEMBER_ID, base)
        val untouched = insertBodyReview(photoId + 1, MEMBER_ID, base)

        // when
        runMigration(DEDUPE_BODY_REVIEW)

        // then
        assertThat(bodyReviewIds()).containsExactlyInAnyOrder(earliest, tieFirst, untouched)
        assertThat(photoCounters(photoId)).containsExactly(3, 12.0)
    }

    @Test
    @DisplayName("리뷰 중복 제거 migration 뒤에는 같은 회원의 같은 사진 리뷰를 두 번 넣을 수 없다")
    fun bodyReviewUniqueIndex() {
        // given
        insertBodyReview(1L, MEMBER_ID, LocalDateTime.now())

        // when
        val duplicate = { insertBodyReview(1L, MEMBER_ID, LocalDateTime.now()); Unit }

        // then
        assertThatThrownBy(duplicate).isInstanceOf(DataIntegrityViolationException::class.java)
    }

    @Test
    @DisplayName("큐 기본키 변경 migration은 기존 행을 보존하고 기본키를 회원과 사진으로 바꾼다")
    fun changeReviewableBodyPhotoPrimaryKey() {
        // given
        restoreLegacyReviewableBodyPhotoKey()
        val createdAt = LocalDateTime.of(2026, 1, 1, 0, 0)
        insertReviewable(MEMBER_ID, 1L, createdAt)
        insertReviewable(MEMBER_ID, 2L, createdAt.plusSeconds(1))

        // when
        runMigration(CHANGE_REVIEWABLE_PRIMARY_KEY)

        // then
        assertThat(primaryKeyColumns("reviewable_body_photo")).containsExactly("member_id", "body_photo_id")
        assertThat(indexNames("reviewable_body_photo")).doesNotContain(REVIEWABLE_LEGACY_UNIQUE_INDEX)
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM reviewable_body_photo", Int::class.java)).isEqualTo(2)
    }

    @Test
    @DisplayName("큐 기본키는 회원과 사진이라 생성 시각이 같은 행도 넣을 수 있고 같은 사진은 두 번 넣을 수 없다")
    fun reviewableBodyPhotoAllowsSameCreatedAt() {
        // given
        val createdAt = LocalDateTime.now()
        insertReviewable(MEMBER_ID, 1L, createdAt)

        // when
        insertReviewable(MEMBER_ID, 2L, createdAt)

        // then
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM reviewable_body_photo", Int::class.java)).isEqualTo(2)
        assertThatThrownBy { insertReviewable(MEMBER_ID, 2L, createdAt.plusSeconds(1)) }
            .isInstanceOf(DataIntegrityViolationException::class.java)
    }

    @Test
    @DisplayName("더미 덱 중복 제거 migration은 회원과 사진마다 가장 작은 dummy_deck_id만 남긴다")
    fun dedupeDummyDeck() {
        // given
        jdbcTemplate.execute("DROP INDEX IF EXISTS $DUMMY_DECK_UNIQUE_INDEX")
        val smallest = insertDummyDeck(MEMBER_ID, 1L)
        insertDummyDeck(MEMBER_ID, 1L)
        insertDummyDeck(MEMBER_ID, 1L)
        val otherPhoto = insertDummyDeck(MEMBER_ID, 2L)
        val otherMember = insertDummyDeck(OTHER_MEMBER_ID, 1L)

        // when
        runMigration(DEDUPE_DUMMY_DECK)

        // then
        val remaining = jdbcTemplate.queryForList("SELECT dummy_deck_id FROM dummy_deck", Long::class.java)
        assertThat(remaining).containsExactlyInAnyOrder(smallest, otherPhoto, otherMember)
    }

    @Test
    @DisplayName("더미 덱 migration 뒤에는 같은 회원의 같은 사진 더미를 두 번 넣을 수 없다")
    fun dummyDeckUniqueIndex() {
        // given
        insertDummyDeck(MEMBER_ID, 1L)

        // when
        val duplicate = { insertDummyDeck(MEMBER_ID, 1L); Unit }

        // then
        assertThatThrownBy(duplicate).isInstanceOf(DataIntegrityViolationException::class.java)
    }

    @Test
    @DisplayName("리뷰 회원 인덱스와 큐 사진 업로더 인덱스가 만들어져 있다")
    fun lookupIndexesExist() {
        // given
        val expected = mapOf(
            "body_review" to "idx_body_review_member_id",
            "reviewable_body_photo" to "idx_reviewable_body_photo_body_photo_member_id",
        )

        // when
        val missing = expected.filter { (table, index) -> index !in indexNames(table) }

        // then
        assertThat(missing).isEmpty()
    }

    @Test
    @DisplayName("큐 초기화 migration은 reviewable_body_photo의 모든 행을 지운다")
    fun clearReviewableBodyPhoto() {
        // given
        val createdAt = LocalDateTime.of(2026, 1, 1, 0, 0)
        insertReviewable(MEMBER_ID, 1L, createdAt)
        insertReviewable(MEMBER_ID, 2L, createdAt)
        insertReviewable(OTHER_MEMBER_ID, 1L, createdAt)

        // when
        runMigration(CLEAR_REVIEWABLE_BODY_PHOTO)

        // then
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM reviewable_body_photo", Int::class.java)).isEqualTo(0)
    }

    private fun restoreLegacyReviewableBodyPhotoKey() {
        val primaryKey = jdbcTemplate.queryForObject(
            "SELECT conname FROM pg_constraint WHERE conrelid = 'reviewable_body_photo'::regclass AND contype = 'p'",
            String::class.java,
        )
        jdbcTemplate.execute("ALTER TABLE reviewable_body_photo DROP CONSTRAINT $primaryKey")
        jdbcTemplate.execute("ALTER TABLE reviewable_body_photo ADD PRIMARY KEY (member_id, created_at)")
        jdbcTemplate.execute(
            "CREATE UNIQUE INDEX IF NOT EXISTS $REVIEWABLE_LEGACY_UNIQUE_INDEX ON reviewable_body_photo (member_id, body_photo_id)",
        )
    }

    private fun runMigration(fileName: String) {
        val sql = ClassPathResource("db/migration/$fileName").inputStream.use { String(it.readAllBytes()) }
        jdbcTemplate.execute(sql)
    }

    private fun primaryKeyColumns(table: String): List<String> = jdbcTemplate.queryForList(
        """
        SELECT a.attname FROM pg_index i
        JOIN pg_attribute a ON a.attrelid = i.indrelid AND a.attnum = ANY (i.indkey)
        WHERE i.indrelid = ?::regclass AND i.indisprimary
        ORDER BY array_position(i.indkey::int2[], a.attnum)
        """.trimIndent(),
        String::class.java, table,
    )

    private fun indexNames(table: String): List<String> =
        jdbcTemplate.queryForList("SELECT indexname FROM pg_indexes WHERE tablename = ?", String::class.java, table)

    private fun bodyReviewIds(): List<Long> =
        jdbcTemplate.queryForList("SELECT body_review_id FROM body_review", Long::class.java)

    private fun photoCounters(bodyPhotoId: Long): List<Any?> = jdbcTemplate.queryForObject(
        "SELECT review_count, total_review_score FROM body_photo WHERE body_photo_id = ?",
        { rs, _ -> listOf(rs.getInt(1), rs.getDouble(2)) }, bodyPhotoId,
    )!!

    private fun insertBodyPhoto(reviewCount: Int, totalReviewScore: Double): Long = jdbcTemplate.queryForObject(
        """
        INSERT INTO body_photo (member_id, image_url, review_count, total_review_score, created_at)
        VALUES (?, 'https://bucket/photo.jpg', ?, ?, ?) RETURNING body_photo_id
        """.trimIndent(),
        Long::class.java, OTHER_MEMBER_ID, reviewCount, totalReviewScore, LocalDateTime.now(),
    )!!

    private fun insertBodyReview(bodyPhotoId: Long, memberId: Long, createdAt: LocalDateTime): Long =
        jdbcTemplate.queryForObject(
            """
            INSERT INTO body_review (body_photo_id, member_id, score, created_at)
            VALUES (?, ?, 4, ?) RETURNING body_review_id
            """.trimIndent(),
            Long::class.java, bodyPhotoId, memberId, createdAt,
        )!!

    private fun insertReviewable(memberId: Long, bodyPhotoId: Long, createdAt: LocalDateTime) {
        jdbcTemplate.update(
            """
            INSERT INTO reviewable_body_photo (member_id, body_photo_id, body_photo_member_id, created_at)
            VALUES (?, ?, ?, ?)
            """.trimIndent(),
            memberId, bodyPhotoId, OTHER_MEMBER_ID, createdAt,
        )
    }

    private fun insertDummyDeck(memberId: Long, bodyPhotoId: Long): Long = jdbcTemplate.queryForObject(
        """
        INSERT INTO dummy_deck (member_id, body_photo_id, body_photo_member_id, nickname, age, created_at)
        VALUES (?, ?, ?, 'dummy', 25, ?) RETURNING dummy_deck_id
        """.trimIndent(),
        Long::class.java, memberId, bodyPhotoId, OTHER_MEMBER_ID, LocalDateTime.now(),
    )!!

    companion object {
        private const val MEMBER_ID = 7_001L
        private const val OTHER_MEMBER_ID = 7_002L
        private const val DEDUPE_BODY_REVIEW = "V23__dedupe_body_review_and_add_unique_index.sql"
        private const val CHANGE_REVIEWABLE_PRIMARY_KEY = "V24__change_reviewable_body_photo_primary_key.sql"
        private const val DEDUPE_DUMMY_DECK = "V25__dedupe_dummy_deck_and_add_unique_index.sql"
        private const val CLEAR_REVIEWABLE_BODY_PHOTO = "V28__clear_reviewable_body_photo.sql"
        private const val BODY_REVIEW_UNIQUE_INDEX = "uq_body_review_body_photo_id_member_id"
        private const val DUMMY_DECK_UNIQUE_INDEX = "uq_dummy_deck_member_id_body_photo_id"
        private const val REVIEWABLE_LEGACY_UNIQUE_INDEX = "idx_reviewable_body_photo_member_id_body_photo_id"
    }

}
