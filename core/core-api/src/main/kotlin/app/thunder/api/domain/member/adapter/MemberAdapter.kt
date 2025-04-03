package app.thunder.api.domain.member.adapter

import app.thunder.api.controller.request.PostSignupRequest
import app.thunder.storage.db.member.MemberEntity
import app.thunder.storage.db.member.MemberRepository
import app.thunder.api.domain.member.repository.MemberSettingRepository
import app.thunder.domain.member.Member
import java.time.LocalDate
import java.time.Period
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class MemberAdapter(
    private val memberRepository: MemberRepository,
    private val memberSettingRepository: MemberSettingRepository
) {

    @Transactional(readOnly = true)
    fun getAll(): List<Member> {
        return memberRepository.findAll()
            .map(::entityToDomain)
    }

    @Transactional(readOnly = true)
    fun getAllByReviewRequestNotifyTrue(): List<Member> {
        val memberIds = memberSettingRepository.findAll().asSequence()
            .filter { it.settings.reviewRequestNotify }
            .map { it.memberId }
            .toList()

        return memberRepository.findAllById(memberIds)
            .map(::entityToDomain)
    }

    @Transactional(readOnly = true)
    fun getById(memberId: Long): Member? {
        return memberRepository.findById(memberId)
            .map(::entityToDomain)
            .orElse(null)
    }

    @Transactional(readOnly = true)
    fun getAllById(memberIds: Collection<Long>): List<Member> {
        return memberRepository.findAllById(memberIds)
            .map(::entityToDomain)
    }

    @Transactional(readOnly = true)
    fun getByNickname(nickname: String): Member? {
        return memberRepository.findByNickname(nickname)
            ?.let { entityToDomain(it) }
    }

    @Transactional(readOnly = true)
    fun getByMobileNumber(mobileNumber: String): Member? {
        return memberRepository.findByMobileNumber(mobileNumber)
            ?.let { entityToDomain(it) }
    }

    @Transactional
    fun create(request: PostSignupRequest): Member {
        val memberEntity = MemberEntity.create(request.nickname,
                                               request.mobileNumber,
                                               request.mobileCountry,
                                               request.gender,
                                               request.birthDay,
                                               request.countryCode,
                                               request.marketingAgreement)
        memberRepository.save(memberEntity)
        return entityToDomain(memberEntity)
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


    private fun entityToDomain(memberEntity: MemberEntity): Member {
        return Member(
            memberEntity.memberId,
            memberEntity.nickname,
            memberEntity.mobileNumber,
            memberEntity.mobileCountry,
            memberEntity.gender,
            memberEntity.birthDay,
            Period.between(memberEntity.birthDay, LocalDate.now()).years,
            memberEntity.countryCode,
            memberEntity.marketingAgreement,
            memberEntity.memberUuid,
            memberEntity.loggedOutAt,
            memberEntity.createdAt,
            memberEntity.updatedAt,
            memberEntity.updatedBy
        )
    }

}