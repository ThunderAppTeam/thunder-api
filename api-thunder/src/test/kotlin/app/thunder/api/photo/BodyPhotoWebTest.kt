package app.thunder.api.photo

import app.thunder.api.ThunderApiWebTest
import app.thunder.core.photo.client.BodyDetectionClient
import app.thunder.core.photo.client.BodyPhotoStorageClient
import java.time.LocalDateTime
import java.util.UUID
import org.junit.jupiter.api.AfterEach
import org.mockito.ArgumentMatchers
import org.springframework.test.context.bean.override.mockito.MockitoBean

abstract class BodyPhotoWebTest : ThunderApiWebTest() {

    @MockitoBean
    protected lateinit var bodyDetectionClient: BodyDetectionClient

    @MockitoBean
    protected lateinit var bodyPhotoStorageClient: BodyPhotoStorageClient

    @AfterEach
    fun deleteBodyFixtures() {
        listOf("reviewable_body_photo", "flag_history", "body_review", "body_photo")
            .forEach { jdbcTemplate.update("DELETE FROM $it") }
    }

    protected fun anyBytes(): ByteArray {
        ArgumentMatchers.any(ByteArray::class.java)
        return ByteArray(0)
    }

    protected fun anyText(): String {
        ArgumentMatchers.anyString()
        return ""
    }

    protected fun insertMemberWithGender(memberId: Long, gender: String) {
        insertMember(memberId)
        jdbcTemplate.update("UPDATE member SET gender = ? WHERE member_id = ?", gender, memberId)
    }

    protected fun insertBodyPhoto(
        memberId: Long,
        reviewCount: Int = 0,
        totalReviewScore: Double = 0.0,
        createdAt: LocalDateTime = LocalDateTime.now(),
        imageUrl: String = "https://test-bucket.s3.ap-northeast-2.amazonaws.com/$memberId/body_photo/${UUID.randomUUID()}.jpg",
    ): Long = jdbcTemplate.queryForObject(
        """
        INSERT INTO body_photo (member_id, image_url, review_count, total_review_score, created_at)
        VALUES (?, ?, ?, ?, ?) RETURNING body_photo_id
        """.trimIndent(),
        Long::class.java, memberId, imageUrl, reviewCount, totalReviewScore, createdAt,
    )!!

    protected fun insertBodyReview(bodyPhotoId: Long, memberId: Long, score: Int) {
        jdbcTemplate.update(
            "INSERT INTO body_review (body_photo_id, member_id, score, created_at) VALUES (?, ?, ?, ?)",
            bodyPhotoId, memberId, score, LocalDateTime.now(),
        )
    }

    protected fun insertFlagHistory(bodyPhotoId: Long, memberId: Long) {
        jdbcTemplate.update(
            """
            INSERT INTO flag_history (member_id, body_photo_id, flag_reason, created_at)
            VALUES (?, ?, 'OTHER', ?)
            """.trimIndent(),
            memberId, bodyPhotoId, LocalDateTime.now(),
        )
    }

    protected fun count(table: String, where: String, vararg args: Any): Int =
        jdbcTemplate.queryForObject("SELECT COUNT(*) FROM $table WHERE $where", Int::class.java, *args)!!

}
