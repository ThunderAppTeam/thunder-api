package app.thunder.api.domain.review.repository

import app.thunder.api.domain.review.entity.DummyDeckEntity
import com.linecorp.kotlinjdsl.support.spring.data.jpa.repository.KotlinJdslJpqlExecutor
import org.springframework.data.jpa.repository.JpaRepository

interface DummyDeckRepository : JpaRepository<DummyDeckEntity, Long>, KotlinJdslJpqlExecutor {
    fun findByMemberIdAndBodyPhotoId(memberId: Long, bodyPhotoId: Long): DummyDeckEntity?
}

fun DummyDeckRepository.findAllByMemberIdOrderByCreatedAt(memberId: Long): List<DummyDeckEntity> {
    return this.findAll {
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
