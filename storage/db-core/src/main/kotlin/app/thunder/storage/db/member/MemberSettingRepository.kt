package app.thunder.storage.db.member

import app.thunder.domain.member.MemberSetting
import app.thunder.domain.member.MemberSettingPort
import app.thunder.domain.member.MemberSettingOptions
import app.thunder.storage.db.member.entity.MemberSettingEntity
import app.thunder.storage.db.member.persistence.MemberSettingJpaRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
internal class MemberSettingRepository(
    private val memberSettingJpaRepository: MemberSettingJpaRepository,
) : MemberSettingPort {

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
