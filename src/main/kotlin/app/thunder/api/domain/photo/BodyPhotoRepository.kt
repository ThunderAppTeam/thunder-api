package app.thunder.api.domain.photo

import app.thunder.api.domain.member.Gender
import app.thunder.api.domain.member.entity.MemberEntity
import app.thunder.api.domain.photo.BodyPhoto.Companion.REVIEW_COMPLETE_COUNT
import app.thunder.api.domain.photo.BodyPhoto.Companion.REVIEW_COMPLETE_DAY
import com.linecorp.kotlinjdsl.support.spring.data.jpa.repository.KotlinJdslJpqlExecutor
import java.time.LocalDateTime
import org.springframework.data.jpa.repository.JpaRepository

interface BodyPhotoRepository : JpaRepository<BodyPhotoEntity, Long>, KotlinJdslJpqlExecutor

fun BodyPhotoRepository.findAllByMemberId(memberId: Long): List<BodyPhotoEntity> {
    return this.findAll {
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

fun BodyPhotoRepository.findAllNotReviewCompleted(): List<BodyPhotoEntity> {
    val before1Day = LocalDateTime.now().minusDays(REVIEW_COMPLETE_DAY)
    return this.findAll {
        select(
            entity(BodyPhotoEntity::class)
        ).from(
            entity(BodyPhotoEntity::class),
        ).whereAnd(
            path(BodyPhotoEntity::createdAt).ge(before1Day),
            path(BodyPhotoEntity::reviewCount).lt(REVIEW_COMPLETE_COUNT),
        ).orderBy(
            path(BodyPhotoEntity::bodyPhotoId).desc()
        )
    }.filterNotNull()
}

fun BodyPhotoRepository.findAllByGender(gender: Gender): List<BodyPhotoEntity> {
    val before30Days = LocalDateTime.now().minusDays(30)

    return this.findAll {
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
