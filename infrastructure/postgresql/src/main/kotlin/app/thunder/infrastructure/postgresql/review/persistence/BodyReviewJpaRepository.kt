package app.thunder.infrastructure.postgresql.review.persistence

import app.thunder.infrastructure.postgresql.review.entity.BodyReviewEntity
import com.linecorp.kotlinjdsl.support.spring.data.jpa.repository.KotlinJdslJpqlExecutor
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

internal interface BodyReviewJpaRepository : JpaRepository<BodyReviewEntity, Long>, KotlinJdslJpqlExecutor {
    fun findAllByMemberId(memberId: Long): List<BodyReviewEntity>
    fun existsByBodyPhotoIdAndMemberId(bodyPhotoId: Long, memberId: Long): Boolean

    @Modifying
    @Query("DELETE FROM BodyReviewEntity r WHERE r.bodyPhotoId = :bodyPhotoId")
    fun deleteAllByBodyPhotoId(@Param("bodyPhotoId") bodyPhotoId: Long)

    fun findLatestGroupByMemberId(): List<BodyReviewEntity> {
        val subQuery = findAll {
            select(
                max(path(BodyReviewEntity::bodyReviewId)),
            ).from(
                entity(BodyReviewEntity::class)
            ).groupBy(
                path(BodyReviewEntity::memberId)
            )
        }

        return findAll {
            select(
                entity(BodyReviewEntity::class)
            ).from(
                entity(BodyReviewEntity::class),
            ).where(
                path(BodyReviewEntity::bodyReviewId).`in`(subQuery)
            )
        }.filterNotNull()
    }
}
