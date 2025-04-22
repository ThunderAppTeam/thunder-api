package app.thunder.infrastructure.postgresql.photo

import app.thunder.core.member.domain.Gender
import app.thunder.core.photo.domain.BodyPhoto.Companion.REVIEW_COMPLETE_COUNT
import app.thunder.infrastructure.postgresql.member.entity.MemberEntity
import com.linecorp.kotlinjdsl.support.spring.data.jpa.repository.KotlinJdslJpqlExecutor
import java.time.LocalDateTime
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

internal interface BodyPhotoJpaRepository : JpaRepository<BodyPhotoEntity, Long>, KotlinJdslJpqlExecutor {

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
        """
        UPDATE BodyPhotoEntity b
        SET b.reviewCount = b.reviewCount + 1,
            b.totalReviewScore = b.totalReviewScore + :score,
            b.updatedAt = :now
        WHERE b.bodyPhotoId = :id AND b.reviewCount < :completeCount
        """
    )
    fun addReview(
        @Param("id") id: Long,
        @Param("score") score: Int,
        @Param("now") now: LocalDateTime,
        @Param("completeCount") completeCount: Int,
    ): Int

    fun findAllByMemberId(memberId: Long): List<BodyPhotoEntity> {
        return findAll {
            select(
                entity(BodyPhotoEntity::class)
            ).from(
                entity(BodyPhotoEntity::class),
            ).whereAnd(
                path(BodyPhotoEntity::memberId).eq(memberId),
            ).orderBy(
                path(BodyPhotoEntity::bodyPhotoId).desc()
            )
        }.filterNotNull()
    }

    fun findAllNotReviewCompleted(): List<BodyPhotoEntity> {
        return findAll {
            select(
                entity(BodyPhotoEntity::class)
            ).from(
                entity(BodyPhotoEntity::class),
            ).whereAnd(
                path(BodyPhotoEntity::reviewCount).lt(REVIEW_COMPLETE_COUNT),
            ).orderBy(
                path(BodyPhotoEntity::bodyPhotoId).desc()
            )
        }.filterNotNull()
    }

    fun countReviewCompletedByGender(gender: Gender, since: LocalDateTime): Long {
        return findAll {
            select(
                count(entity(BodyPhotoEntity::class))
            ).from(
                entity(BodyPhotoEntity::class),
                join(entity(MemberEntity::class))
                    .on(path(MemberEntity::memberId).eq(path(BodyPhotoEntity::memberId)))
            ).whereAnd(
                path(BodyPhotoEntity::createdAt).ge(since),
                path(BodyPhotoEntity::reviewCount).ge(REVIEW_COMPLETE_COUNT),
                path(MemberEntity::gender).eq(gender),
            )
        }.firstOrNull() ?: 0L
    }

    fun countReviewCompletedByGenderWithHigherAverageScore(
        gender: Gender,
        averageScore: Double,
        since: LocalDateTime,
    ): Long {
        return findAll {
            select(
                count(entity(BodyPhotoEntity::class))
            ).from(
                entity(BodyPhotoEntity::class),
                join(entity(MemberEntity::class))
                    .on(path(MemberEntity::memberId).eq(path(BodyPhotoEntity::memberId)))
            ).whereAnd(
                path(BodyPhotoEntity::createdAt).ge(since),
                path(BodyPhotoEntity::reviewCount).ge(REVIEW_COMPLETE_COUNT),
                path(MemberEntity::gender).eq(gender),
                customExpression(
                    Double::class,
                    "CASE WHEN {1} = 0 THEN 0.0 ELSE {0} / {1} END",
                    path(BodyPhotoEntity::totalReviewScore),
                    path(BodyPhotoEntity::reviewCount),
                ).gt(averageScore),
            )
        }.firstOrNull() ?: 0L
    }
}
