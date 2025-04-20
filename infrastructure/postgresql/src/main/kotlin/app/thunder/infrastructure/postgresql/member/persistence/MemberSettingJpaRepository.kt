package app.thunder.infrastructure.postgresql.member.persistence

import app.thunder.infrastructure.postgresql.member.entity.MemberSettingEntity
import org.springframework.data.jpa.repository.JpaRepository

internal interface MemberSettingJpaRepository : JpaRepository<MemberSettingEntity, Long>
