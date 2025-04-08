package app.thunder.storage.db.member.persistence

import app.thunder.storage.db.member.entity.MemberSettingEntity
import org.springframework.data.jpa.repository.JpaRepository

internal interface MemberSettingJpaRepository : JpaRepository<MemberSettingEntity, Long>
