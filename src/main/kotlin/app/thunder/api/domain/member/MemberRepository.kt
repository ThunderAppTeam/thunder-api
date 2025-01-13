package app.thunder.api.domain.member

import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface MemberRepository : JpaRepository<MemberEntity, Long> {
    fun findByMobileNumber(mobileNumber: String): Optional<MemberEntity>
    fun findByNickname(nickName: String): Optional<MemberEntity>
}