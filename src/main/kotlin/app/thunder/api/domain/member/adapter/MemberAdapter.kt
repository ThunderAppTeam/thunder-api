package app.thunder.api.domain.member.adapter

import app.thunder.api.controller.request.PostSignupRequest
import app.thunder.api.domain.member.Member
import app.thunder.api.domain.member.entity.MemberEntity
import app.thunder.api.domain.member.repository.MemberRepository
import app.thunder.api.exception.MemberErrors.NOT_FOUND_MEMBER
import app.thunder.api.exception.ThunderException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class MemberAdapter(
    private val memberRepository: MemberRepository
) {

    @Transactional(readOnly = true)
    fun getById(memberId: Long): Member {
        val memberEntity = memberRepository.findById(memberId)
            .orElseThrow { ThunderException(NOT_FOUND_MEMBER) }
        return Member.from(memberEntity)
    }

    @Transactional(readOnly = true)
    fun getAllById(memberIds: Collection<Long>): List<Member> {
        return memberRepository.findAllById(memberIds)
            .map(Member::from)
    }

    @Transactional(readOnly = true)
    fun getByNickname(nickname: String): Member? {
        return memberRepository.findByNickname(nickname)
            ?.let { Member.from(it) }
    }

    @Transactional(readOnly = true)
    fun getByMobileNumber(mobileNumber: String): Member? {
        return memberRepository.findByMobileNumber(mobileNumber)
            ?.let { Member.from(it) }
    }

    @Transactional
    fun create(request: PostSignupRequest): Member {
        val newMember = MemberEntity.create(request.nickname,
                                            request.mobileNumber,
                                            request.mobileCountry,
                                            request.gender,
                                            request.birthDay,
                                            request.countryCode,
                                            request.marketingAgreement)
        memberRepository.save(newMember)
        return Member.from(newMember)
    }

    @Transactional
    fun update(member: Member) {
        memberRepository.findById(member.memberId)
            .map { memberEntity ->
                memberEntity.update(loggedOutAt = member.loggedOutAt,
                                    updatedAt = member.updatedAt,
                                    updatedBy = member.updatedBy)
            }
    }

    @Transactional
    fun deleteById(memberId: Long) {
        memberRepository.deleteById(memberId)
    }

}