package app.thunder.infrastructure.db.member.persistence

import app.thunder.infrastructure.db.member.entity.MemberSettingEntity
import org.springframework.data.jpa.repository.JpaRepository

internal interface MemberSettingJpaRepository : JpaRepository<MemberSettingEntity, Long>
