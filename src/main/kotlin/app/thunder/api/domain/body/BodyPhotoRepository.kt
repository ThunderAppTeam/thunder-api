package app.thunder.api.domain.body

import app.thunder.api.domain.member.Gender
import app.thunder.api.domain.member.MemberEntity
import com.linecorp.kotlinjdsl.support.spring.data.jpa.repository.KotlinJdslJpqlExecutor
import java.time.LocalDateTime
import org.springframework.data.jpa.repository.JpaRepository

interface BodyPhotoRepository : JpaRepository<BodyPhotoEntity, Long>, KotlinJdslJpqlExecutor

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
            path(BodyPhotoEntity::reviewScore).desc()
        )
    }.filterNotNull()
}