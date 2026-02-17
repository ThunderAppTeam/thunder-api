package app.thunder.infrastructure.db.review.persistence

import app.thunder.infrastructure.db.review.entity.DummyDeckEntity
import com.linecorp.kotlinjdsl.support.spring.data.jpa.repository.KotlinJdslJpqlExecutor
import org.springframework.data.jpa.repository.JpaRepository

internal interface DummyDeckJpaRepository : JpaRepository<DummyDeckEntity, Long>, KotlinJdslJpqlExecutor {
    fun findByMemberIdAndBodyPhotoId(memberId: Long, bodyPhotoId: Long): DummyDeckEntity?

    fun findAllByMemberIdOrderByCreatedAt(memberId: Long): List<DummyDeckEntity> {
        return findAll {
            select(
                entity(DummyDeckEntity::class)
            ).from(
                entity(DummyDeckEntity::class)
            ).where(
                path(DummyDeckEntity::memberId).eq(memberId)
            ).orderBy(
                path(DummyDeckEntity::createdAt).asc()
            )
        }.filterNotNull()
    }
}
