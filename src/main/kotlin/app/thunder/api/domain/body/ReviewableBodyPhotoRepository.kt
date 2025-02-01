package app.thunder.api.domain.body

import com.linecorp.kotlinjdsl.support.spring.data.jpa.repository.KotlinJdslJpqlExecutor
import org.springframework.data.jpa.repository.JpaRepository

interface ReviewableBodyPhotoRepository : JpaRepository<ReviewableBodyPhotoEntity, ReviewableBodyPhotoId>,
                                          KotlinJdslJpqlExecutor {
    fun deleteByMemberIdAndBodyPhotoId(memberId: Long, bodyPhotoId: Long)
}

fun ReviewableBodyPhotoRepository.findAllByMemberId(memberId: Long): List<ReviewableBodyPhotoEntity> {
    return this.findAll {
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