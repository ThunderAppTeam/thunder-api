package app.thunder.api.domain.review.repository

import app.thunder.api.domain.review.entity.DummyDeckEntity
import java.sql.PreparedStatement
import java.sql.Timestamp
import org.springframework.jdbc.core.BatchPreparedStatementSetter
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class DummyDeckJdbcRepository(
    private val jdbcTemplate: JdbcTemplate
) {

    @Transactional
    fun batchInsert(entities: List<DummyDeckEntity>) {
        val sql = """
            INSERT INTO dummy_deck (
                member_id,
                body_photo_id,
                body_photo_member_id,
                nickname,
                age,
                created_at
            ) 
            VALUES (?, ?, ?, ?, ?, ?)
        """

        jdbcTemplate.batchUpdate(sql, object : BatchPreparedStatementSetter {
            override fun setValues(ps: PreparedStatement, i: Int) {
                val entity = entities[i]
                ps.setLong(1, entity.memberId)
                ps.setLong(2, entity.bodyPhotoId)
                ps.setLong(3, entity.bodyPhotoMemberId)
                ps.setString(4, entity.nickname)
                ps.setInt(5, entity.age)
                ps.setTimestamp(6, Timestamp.valueOf(entity.createdAt))
            }

            override fun getBatchSize(): Int = entities.size
        })
    }

}
