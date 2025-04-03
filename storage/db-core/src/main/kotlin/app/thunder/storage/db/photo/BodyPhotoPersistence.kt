package app.thunder.storage.db.photo

import app.thunder.domain.member.Gender
import app.thunder.domain.photo.BodyPhoto.Companion.REVIEW_COMPLETE_COUNT
import app.thunder.storage.db.member.MemberEntity
import com.linecorp.kotlinjdsl.support.spring.data.jpa.repository.KotlinJdslJpqlExecutor
import java.time.LocalDateTime
import org.springframework.data.jpa.repository.JpaRepository

internal interface BodyPhotoPersistence : JpaRepository<BodyPhotoEntity, Long>, KotlinJdslJpqlExecutor

internal fun BodyPhotoPersistence.findAllByMemberId(memberId: Long): List<BodyPhotoEntity> {
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

internal fun BodyPhotoPersistence.findAllNotReviewCompleted(): List<BodyPhotoEntity> {
    return this.findAll {
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

internal fun BodyPhotoPersistence.findAllByGender(gender: Gender): List<BodyPhotoEntity> {
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
