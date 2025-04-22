package app.thunder.infrastructure.postgresql.member

import app.thunder.core.member.domain.Member
import app.thunder.core.member.repository.MemberRepository
import app.thunder.core.member.command.CreateMemberCommand
import app.thunder.infrastructure.postgresql.member.entity.MemberEntity
import app.thunder.infrastructure.postgresql.member.persistence.MemberJpaRepository
import app.thunder.infrastructure.postgresql.member.persistence.MemberSettingJpaRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Period
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
internal class MemberRepositoryImpl(
    private val memberJpaRepository: MemberJpaRepository,
    private val memberSettingJpaRepository: MemberSettingJpaRepository
) : MemberRepository {

    @Transactional(readOnly = true)
    override fun getAll(): List<Member> {
        return memberJpaRepository.findAll()
            .map(::entityToDomain)
    }

    @Transactional(readOnly = true)
    override fun getAllByReviewRequestNotifyTrue(): List<Member> {
        val memberIds = memberSettingJpaRepository.findAll().asSequence()
            .filter { it.settings.reviewRequestNotify }
            .map { it.memberId }
            .toList()

        return memberJpaRepository.findAllById(memberIds)
            .map(::entityToDomain)
    }

    @Transactional(readOnly = true)
    override fun getById(memberId: Long): Member? {
        return memberJpaRepository.findById(memberId)
            .map(::entityToDomain)
            .orElse(null)
    }

    @Transactional(readOnly = true)
    override fun getAllById(memberIds: Collection<Long>): List<Member> {
        return memberJpaRepository.findAllById(memberIds)
            .map(::entityToDomain)
    }

    @Transactional(readOnly = true)
    override fun getByNickname(nickname: String): Member? {
        return memberJpaRepository.findByNickname(nickname)
            ?.let { entityToDomain(it) }
    }

    @Transactional(readOnly = true)
    override fun getByMobileNumber(mobileNumber: String): Member? {
        return memberJpaRepository.findByMobileNumber(mobileNumber)
            ?.let { entityToDomain(it) }
    }

    @Transactional
    override fun create(command: CreateMemberCommand): Member {
        val memberEntity = MemberEntity.create(command.nickname,
                                               command.mobileNumber,
                                               command.mobileCountry,
                                               command.gender,
                                               command.birthDay,
                                               command.countryCode,
                                               command.marketingAgreement)
        memberJpaRepository.save(memberEntity)
        return entityToDomain(memberEntity)
    }

    @Transactional
    override fun revokeTokensIssuedBefore(memberId: Long, revokedAt: LocalDateTime) {
        memberJpaRepository.revokeTokensIssuedBefore(memberId, revokedAt)
    }

    @Transactional
    override fun deleteById(memberId: Long) {
        memberJpaRepository.deleteById(memberId)
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
