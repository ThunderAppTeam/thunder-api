package app.thunder.api.domain.member.repository

import app.thunder.api.domain.member.entity.MemberEntity
import org.springframework.data.jpa.repository.JpaRepository

interface MemberRepository : JpaRepository<MemberEntity, Long> {
    fun findByMobileNumber(mobileNumber: String): MemberEntity?
    fun findByNickname(nickName: String): MemberEntity?
}