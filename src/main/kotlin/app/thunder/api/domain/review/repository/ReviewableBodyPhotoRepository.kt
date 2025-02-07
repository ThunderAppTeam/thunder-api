package app.thunder.api.domain.review.repository

import app.thunder.api.domain.review.entity.ReviewableBodyPhotoEntity
import app.thunder.api.domain.review.entity.ReviewableBodyPhotoId
import com.linecorp.kotlinjdsl.support.spring.data.jpa.repository.KotlinJdslJpqlExecutor
import org.springframework.data.jpa.repository.JpaRepository

interface ReviewableBodyPhotoRepository : JpaRepository<ReviewableBodyPhotoEntity, ReviewableBodyPhotoId>,
                                          KotlinJdslJpqlExecutor {
    fun findAllByBodyPhotoId(bodyPhotoId: Long): List<ReviewableBodyPhotoEntity>
    fun findAllByBodyPhotoMemberId(bodyPhotoMemberId: Long): List<ReviewableBodyPhotoEntity>
    fun deleteByMemberIdAndBodyPhotoId(memberId: Long, bodyPhotoId: Long)
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
