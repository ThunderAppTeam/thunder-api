package app.thunder.infrastructure.db.member.persistence

import app.thunder.infrastructure.db.member.entity.DeletedMemberEntity
import org.springframework.data.jpa.repository.JpaRepository

internal interface DeletedMemberJpaRepository : JpaRepository<DeletedMemberEntity, Long>
