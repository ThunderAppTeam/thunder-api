package app.thunder.storage.db.member.persistence

import app.thunder.storage.db.member.entity.DeletedMemberEntity
import org.springframework.data.jpa.repository.JpaRepository

internal interface DeletedMemberJpaRepository : JpaRepository<DeletedMemberEntity, Long>
