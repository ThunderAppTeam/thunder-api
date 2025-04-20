package app.thunder.infrastructure.postgresql.photo

import app.thunder.core.member.domain.Gender
import app.thunder.core.photo.domain.BodyPhoto.Companion.REVIEW_COMPLETE_COUNT
import app.thunder.infrastructure.postgresql.member.entity.MemberEntity
import com.linecorp.kotlinjdsl.support.spring.data.jpa.repository.KotlinJdslJpqlExecutor
import java.time.LocalDateTime
import org.springframework.data.jpa.repository.JpaRepository

internal interface BodyPhotoJpaRepository : JpaRepository<BodyPhotoEntity, Long>, KotlinJdslJpqlExecutor {

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

    fun findAllByGender(gender: Gender): List<BodyPhotoEntity> {
        val before30Days = LocalDateTime.now().minusDays(30)

        return findAll {
            select(
                entity(BodyPhotoEntity::class)
            ).from(
                entity(BodyPhotoEntity::class),
                fetchJoin(entity(MemberEntity::class))
                    .on(path(MemberEntity::memberId).eq(path(BodyPhotoEntity::memberId)))
            ).whereAnd(
                path(BodyPhotoEntity::createdAt).ge(before30Days),
                path(MemberEntity::gender).eq(gender),
            ).orderBy(
                path(BodyPhotoEntity::totalReviewScore).desc()
            )
        }.filterNotNull()
    }

    fun countByGender(gender: Gender): Long {
        val before30Days = LocalDateTime.now().minusDays(30)

        return findAll {
            select(
                count(entity(BodyPhotoEntity::class))
            ).from(
                entity(BodyPhotoEntity::class),
                join(entity(MemberEntity::class))
                    .on(path(MemberEntity::memberId).eq(path(BodyPhotoEntity::memberId)))
            ).whereAnd(
                path(BodyPhotoEntity::createdAt).ge(before30Days),
                path(MemberEntity::gender).eq(gender),
            )
        }.firstOrNull() ?: 0L
    }

    fun countByGenderWithHigherScore(gender: Gender, score: Double): Long {
        val before30Days = LocalDateTime.now().minusDays(30)

        return findAll {
            select(
                count(entity(BodyPhotoEntity::class))
            ).from(
                entity(BodyPhotoEntity::class),
                join(entity(MemberEntity::class))
                    .on(path(MemberEntity::memberId).eq(path(BodyPhotoEntity::memberId)))
            ).whereAnd(
                path(BodyPhotoEntity::createdAt).ge(before30Days),
                path(MemberEntity::gender).eq(gender),
                path(BodyPhotoEntity::totalReviewScore).gt(score),
            )
        }.firstOrNull() ?: 0L
    }
}
