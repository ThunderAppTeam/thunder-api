package app.thunder.api.domain.member.repository

import app.thunder.api.domain.member.entity.MemberFcmTokenEntity
import org.springframework.data.jpa.repository.JpaRepository

interface MemberFcmTokenRepository : JpaRepository<MemberFcmTokenEntity, Long>