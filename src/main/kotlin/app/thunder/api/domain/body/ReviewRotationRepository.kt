package app.thunder.api.domain.body

import com.linecorp.kotlinjdsl.support.spring.data.jpa.repository.KotlinJdslJpqlExecutor
import org.springframework.data.jpa.repository.JpaRepository

interface ReviewRotationQueueRepository : JpaRepository<ReviewRotationEntity, Long>, KotlinJdslJpqlExecutor {
    fun findAllByBodyPhotoIdIn(bodyPhotoIds: Collection<Long>): List<ReviewRotationEntity>
}

fun ReviewRotationQueueRepository.getAllByIdGteAndMemberIdNot(
    reviewRotationId: Long,
    memberId: Long,
    fetchSize: Int
): List<ReviewRotationEntity> {
    return this.findAll(offset = 0, limit = fetchSize) {
        select(
            entity(ReviewRotationEntity::class)
        ).from(
            entity(ReviewRotationEntity::class)
        ).whereAnd(
            path(ReviewRotationEntity::reviewRotationId).ge(reviewRotationId),
            path(ReviewRotationEntity::memberId).notEqual(memberId)
        ).orderBy(
            path(ReviewRotationEntity::reviewRotationId).asc()
        )
    }.filterNotNull()
}
