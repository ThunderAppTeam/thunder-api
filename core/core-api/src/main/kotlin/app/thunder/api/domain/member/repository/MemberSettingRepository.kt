package app.thunder.api.domain.member.repository

import app.thunder.api.domain.member.entity.MemberSettingEntity
import org.springframework.data.jpa.repository.JpaRepository

interface MemberSettingRepository : JpaRepository<MemberSettingEntity, Long>