package app.thunder.api.domain.member.adapter

import app.thunder.api.controller.request.PostSignupRequest
import app.thunder.api.domain.member.Member
import app.thunder.api.domain.member.entity.MemberEntity
import app.thunder.api.domain.member.repository.MemberRepository
import app.thunder.api.domain.member.repository.MemberSettingRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class MemberAdapter(
    private val memberRepository: MemberRepository,
    private val memberSettingRepository: MemberSettingRepository
) {

    @Transactional(readOnly = true)
    fun getAll(): List<Member> {
        return memberRepository.findAll().map(Member::from)
    }

    @Transactional(readOnly = true)
    fun getAllByReviewRequestNotifyTrue(): List<Member> {
        val memberIds = memberSettingRepository.findAll().asSequence()
            .filter { it.settings.reviewRequestNotify }
            .map { it.memberId }
            .toList()

        return memberRepository.findAllById(memberIds)
            .map(Member::from)
    }

    @Transactional(readOnly = true)
    fun getById(memberId: Long): Member? {
        return memberRepository.findById(memberId)
            .map(Member::from)
            .orElse(null)
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