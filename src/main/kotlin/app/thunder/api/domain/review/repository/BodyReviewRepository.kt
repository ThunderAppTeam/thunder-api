package app.thunder.api.domain.review.repository

import app.thunder.api.domain.review.entity.BodyReviewEntity
import com.linecorp.kotlinjdsl.support.spring.data.jpa.repository.KotlinJdslJpqlExecutor
import org.springframework.data.jpa.repository.JpaRepository

interface BodyReviewRepository : JpaRepository<BodyReviewEntity, Long>, KotlinJdslJpqlExecutor {
    fun existsByBodyPhotoIdAndMemberId(bodyPhotoId: Long, memberId: Long): Boolean
    fun findAllByMemberId(memberId: Long): List<BodyReviewEntity>
}

fun BodyReviewRepository.findLatestGroupByMemberId(): List<BodyReviewEntity> {
    val subQuery = this.findAll {
        select(
            max(path(BodyReviewEntity::bodyReviewId)),
        ).from(
            entity(BodyReviewEntity::class)
        ).groupBy(
            path(BodyReviewEntity::memberId)
        )
    }

    return this.findAll {
        select(
            entity(BodyReviewEntity::class)
        ).from(
            entity(BodyReviewEntity::class),
        ).where(
            path(BodyReviewEntity::bodyReviewId).`in`(subQuery)
        )
    }.filterNotNull()
}
