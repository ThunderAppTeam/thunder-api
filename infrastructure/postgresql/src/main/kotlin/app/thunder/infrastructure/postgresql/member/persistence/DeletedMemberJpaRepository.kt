package app.thunder.infrastructure.postgresql.member.persistence

import app.thunder.infrastructure.postgresql.member.entity.DeletedMemberEntity
import org.springframework.data.jpa.repository.JpaRepository

internal interface DeletedMemberJpaRepository : JpaRepository<DeletedMemberEntity, Long>
