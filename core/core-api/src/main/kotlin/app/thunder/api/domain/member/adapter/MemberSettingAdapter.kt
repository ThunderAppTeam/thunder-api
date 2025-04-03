package app.thunder.api.domain.member.adapter

import app.thunder.api.domain.member.entity.MemberSettingEntity
import app.thunder.api.domain.member.repository.MemberSettingRepository
import app.thunder.domain.member.MemberSetting
import app.thunder.domain.member.MemberSettingOptions
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class MemberSettingAdapter(
    private val memberSettingRepository: MemberSettingRepository,
) {

    @Transactional(readOnly = true)
    fun getByMemberId(memberId: Long): MemberSetting? {
        return memberSettingRepository.findById(memberId)
            .map(::entityToDomain)
            .orElse(null)
    }

    @Transactional
    fun create(
        memberId: Long,
        memberSettingOptions: MemberSettingOptions,
    ): MemberSetting {
        val entity = MemberSettingEntity.create(memberId, memberSettingOptions)
        memberSettingRepository.save(entity)
        return entityToDomain(entity)
    }

    @Transactional
    fun update(memberSetting: MemberSetting) {
        memberSettingRepository.findById(memberSetting.memberId)
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