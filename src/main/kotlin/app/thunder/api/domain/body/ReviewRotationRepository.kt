package app.thunder.api.domain.body

import com.linecorp.kotlinjdsl.support.spring.data.jpa.repository.KotlinJdslJpqlExecutor
import java.util.Optional
import org.springframework.data.jpa.repository.JpaRepository

interface ReviewRotationQueueRepository : JpaRepository<ReviewRotationEntity, Long>, KotlinJdslJpqlExecutor {
    fun findByBodyPhotoId(bodyPhotoId: Long): Optional<ReviewRotationEntity>
    fun findAllByBodyPhotoIdIn(bodyPhotoIds: Collection<Long>): List<ReviewRotationEntity>
}

fun ReviewRotationQueueRepository.getAllByIdCursor(
    reviewRotationId: Long,
    fetchSize: Int
): List<ReviewRotationEntity> {
    return this.findAll(offset = 0, limit = fetchSize) {
        select(
            entity(ReviewRotationEntity::class)
        ).from(
            entity(ReviewRotationEntity::class)
        ).whereAnd(
            path(ReviewRotationEntity::reviewRotationId).ge(reviewRotationId)
        )
    }.filterNotNull()
}
