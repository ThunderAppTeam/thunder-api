package app.thunder.api.domain.review.repository

import app.thunder.api.domain.review.entity.ReviewableBodyPhotoEntity
import app.thunder.api.domain.review.entity.ReviewableBodyPhotoId
import com.linecorp.kotlinjdsl.support.spring.data.jpa.repository.KotlinJdslJpqlExecutor
import jakarta.persistence.Tuple
import java.time.LocalDateTime
import org.springframework.data.jpa.repository.JpaRepository

interface ReviewableBodyPhotoRepository : JpaRepository<ReviewableBodyPhotoEntity, ReviewableBodyPhotoId>,
                                          KotlinJdslJpqlExecutor {
    fun findAllByBodyPhotoId(bodyPhotoId: Long): List<ReviewableBodyPhotoEntity>
    fun findAllByBodyPhotoMemberId(bodyPhotoMemberId: Long): List<ReviewableBodyPhotoEntity>
    fun deleteByMemberIdAndBodyPhotoId(memberId: Long, bodyPhotoId: Long)
}

fun ReviewableBodyPhotoRepository.findFirstAllByMemberIds(memberIds: Collection<Long>): List<ReviewableBodyPhotoEntity> {
    val subQuery = this.findAll {
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

    return this.findAll {
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

fun ReviewableBodyPhotoRepository.findAllByMemberId(
    memberId: Long,
    limit: Int? = null,
): List<ReviewableBodyPhotoEntity> {
    return this.findAll(limit = limit) {
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
