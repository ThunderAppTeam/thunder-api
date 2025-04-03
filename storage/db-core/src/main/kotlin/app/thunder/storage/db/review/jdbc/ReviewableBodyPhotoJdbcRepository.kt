package app.thunder.storage.db.review.jdbc

import app.thunder.storage.db.review.entity.ReviewableBodyPhotoEntity
import java.sql.PreparedStatement
import java.sql.Timestamp
import org.springframework.jdbc.core.BatchPreparedStatementSetter
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
internal class ReviewableBodyPhotoJdbcRepository(
    private val jdbcTemplate: JdbcTemplate
) {

    @Transactional
    fun batchInsert(entities: List<ReviewableBodyPhotoEntity>) {
        val sql = """
            INSERT INTO reviewable_body_photo (
                member_id,
                body_photo_id,
                body_photo_member_id,
                created_at
            ) 
            VALUES (?, ?, ?, ?)
            ON CONFLICT (member_id, body_photo_id) DO NOTHING
        """

        jdbcTemplate.batchUpdate(sql, object : BatchPreparedStatementSetter {
            override fun setValues(ps: PreparedStatement, i: Int) {
                val entity = entities[i]
                ps.setLong(1, entity.memberId)
                ps.setLong(2, entity.bodyPhotoId)
                ps.setLong(3, entity.bodyPhotoMemberId)
                ps.setTimestamp(4, Timestamp.valueOf(entity.createdAt))
            }

            override fun getBatchSize(): Int = entities.size
        })
    }

}
