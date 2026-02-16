package app.thunder.storage.db.review.persistence

import app.thunder.storage.db.review.entity.BodyReviewEntity
import com.linecorp.kotlinjdsl.support.spring.data.jpa.repository.KotlinJdslJpqlExecutor
import org.springframework.data.jpa.repository.JpaRepository

internal interface BodyReviewJpaRepository : JpaRepository<BodyReviewEntity, Long>, KotlinJdslJpqlExecutor {
    fun findAllByMemberId(memberId: Long): List<BodyReviewEntity>
    fun existsByBodyPhotoIdAndMemberId(bodyPhotoId: Long, memberId: Long): Boolean
}

internal fun BodyReviewJpaRepository.findLatestGroupByMemberId(): List<BodyReviewEntity> {
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
