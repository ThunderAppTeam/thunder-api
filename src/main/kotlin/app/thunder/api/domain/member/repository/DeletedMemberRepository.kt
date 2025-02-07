package app.thunder.api.domain.member.repository

import app.thunder.api.domain.member.entity.DeletedMemberEntity
import org.springframework.data.jpa.repository.JpaRepository

interface DeletedMemberRepository : JpaRepository<DeletedMemberEntity, Long>