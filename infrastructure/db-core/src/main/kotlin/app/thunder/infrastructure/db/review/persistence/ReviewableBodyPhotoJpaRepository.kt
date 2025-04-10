package app.thunder.infrastructure.db.review.persistence

import app.thunder.infrastructure.db.review.entity.ReviewableBodyPhotoEntity
import app.thunder.infrastructure.db.review.entity.ReviewableBodyPhotoId
import com.linecorp.kotlinjdsl.support.spring.data.jpa.repository.KotlinJdslJpqlExecutor
import jakarta.persistence.Tuple
import java.time.LocalDateTime
import org.springframework.data.jpa.repository.JpaRepository

internal interface ReviewableBodyPhotoJpaRepository : JpaRepository<ReviewableBodyPhotoEntity, ReviewableBodyPhotoId>,
                                                       KotlinJdslJpqlExecutor {
    fun findAllByBodyPhotoId(bodyPhotoId: Long): List<ReviewableBodyPhotoEntity>
    fun findAllByBodyPhotoMemberId(bodyPhotoMemberId: Long): List<ReviewableBodyPhotoEntity>
    fun deleteByMemberIdAndBodyPhotoId(memberId: Long, bodyPhotoId: Long)

    fun findFirstAllByMemberIds(memberIds: Collection<Long>): List<ReviewableBodyPhotoEntity> {
        val subQuery = findAll {
            select<Tuple>(
                path(ReviewableBodyPhotoEntity::memberId),
                min(path(ReviewableBodyPhotoEntity::createdAt))
            ).from(
                entity(ReviewableBodyPhotoEntity::class),
            ).where(
                path(ReviewableBodyPhotoEntity::memberId).`in`(memberIds)
            ).groupBy(
                path(ReviewableBodyPhotoEntity::memberId)
            )
        }

        return findAll {
            select(
                entity(ReviewableBodyPhotoEntity::class),
            ).from(
                entity(ReviewableBodyPhotoEntity::class),
            ).whereOr(
                *subQuery.map { tuple ->
                    val memberId = tuple?.get(0, Long::class.java)
                    val createdAt = tuple?.get(1, LocalDateTime::class.java)
                    and(
                        path(ReviewableBodyPhotoEntity::memberId).eq(memberId),
                        path(ReviewableBodyPhotoEntity::createdAt).eq(createdAt),
                    )
                }.toTypedArray()
            )
        }.filterNotNull()
    }

    fun findAllByMemberId(
        memberId: Long,
        limit: Int? = null,
    ): List<ReviewableBodyPhotoEntity> {
        return findAll(limit = limit) {
            select(
                entity(ReviewableBodyPhotoEntity::class)
            ).from(
                entity(ReviewableBodyPhotoEntity::class),
            ).whereAnd(
                path(ReviewableBodyPhotoEntity::memberId).eq(memberId),
            ).orderBy(
                path(ReviewableBodyPhotoEntity::createdAt).asc()
            )
        }.filterNotNull()
    }
}
