package app.thunder.infrastructure.db.member.persistence

import app.thunder.infrastructure.db.member.entity.MemberEntity
import org.springframework.data.jpa.repository.JpaRepository

internal interface MemberJpaRepository : JpaRepository<MemberEntity, Long> {
    fun findByMobileNumber(mobileNumber: String): MemberEntity?
    fun findByNickname(nickName: String): MemberEntity?
}
