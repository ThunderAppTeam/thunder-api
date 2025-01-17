package app.thunder.api.domain.member

import app.thunder.api.exception.MemberErrors.NOT_FOUND_MEMBER
import app.thunder.api.exception.ThunderException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class MemberAdapter(
    private val memberRepository: MemberRepository
) {

    @Transactional(readOnly = true)
    fun getByMemberId(memberId: Long): Member {
        val memberEntity = memberRepository.findById(memberId)
            .orElseThrow { ThunderException(NOT_FOUND_MEMBER) }
        return Member.from(memberEntity)
    }

}