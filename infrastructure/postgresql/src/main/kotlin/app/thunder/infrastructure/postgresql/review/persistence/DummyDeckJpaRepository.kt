package app.thunder.infrastructure.postgresql.review.persistence

import app.thunder.infrastructure.postgresql.review.entity.DummyDeckEntity
import com.linecorp.kotlinjdsl.support.spring.data.jpa.repository.KotlinJdslJpqlExecutor
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

internal interface DummyDeckJpaRepository : JpaRepository<DummyDeckEntity, Long>, KotlinJdslJpqlExecutor {
    fun findByMemberIdAndBodyPhotoId(memberId: Long, bodyPhotoId: Long): DummyDeckEntity?

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM DummyDeckEntity d WHERE d.memberId = :memberId")
    fun deleteAllByMemberIdInBulk(@Param("memberId") memberId: Long): Int

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
