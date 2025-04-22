package app.thunder.infrastructure.postgresql.member

import app.thunder.core.member.domain.MemberSetting
import app.thunder.core.member.domain.MemberSettingOptions
import app.thunder.core.member.repository.MemberSettingRepository
import app.thunder.infrastructure.postgresql.member.entity.MemberSettingEntity
import app.thunder.infrastructure.postgresql.member.persistence.MemberSettingJpaRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
internal class MemberSettingRepositoryImpl(
    private val memberSettingJpaRepository: MemberSettingJpaRepository,
) : MemberSettingRepository {

    @Transactional(readOnly = true)
    override fun getByMemberId(memberId: Long): MemberSetting? {
        return memberSettingJpaRepository.findById(memberId)
            .map(::entityToDomain)
            .orElse(null)
    }

    @Transactional
    override fun create(
        memberId: Long,
        memberSettingOptions: MemberSettingOptions,
    ): MemberSetting {
        val entity = MemberSettingEntity.create(memberId, memberSettingOptions)
        memberSettingJpaRepository.save(entity)
        return entityToDomain(entity)
    }

    @Transactional
    override fun update(memberSetting: MemberSetting) {
        memberSettingJpaRepository.findById(memberSetting.memberId)
            .map { memberSettingEntity ->
                val options = MemberSettingOptions(
                    reviewCompleteNotify = memberSetting.reviewCompleteNotify,
                    reviewRequestNotify = memberSetting.reviewRequestNotify,
                    marketingAgreement = memberSetting.marketingAgreement,
                )
                memberSettingEntity.update(options, memberSetting.updatedAt)
            }
    }

    @Transactional
    override fun deleteByMemberId(memberId: Long) {
        memberSettingJpaRepository.deleteById(memberId)
    }

    private fun entityToDomain(entity: MemberSettingEntity): MemberSetting {
        return MemberSetting(
            memberId = entity.memberId,
            reviewCompleteNotify = entity.settings.reviewCompleteNotify,
            reviewRequestNotify = entity.settings.reviewRequestNotify,
            marketingAgreement = entity.settings.marketingAgreement,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
        )
    }

}
