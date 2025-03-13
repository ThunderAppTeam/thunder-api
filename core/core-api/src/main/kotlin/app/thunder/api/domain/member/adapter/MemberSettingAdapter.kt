package app.thunder.api.domain.member.adapter

import app.thunder.api.domain.member.MemberSetting
import app.thunder.api.domain.member.MemberSettingOptions
import app.thunder.api.domain.member.entity.MemberSettingEntity
import app.thunder.api.domain.member.repository.MemberSettingRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class MemberSettingAdapter(
    private val memberSettingRepository: MemberSettingRepository,
) {

    @Transactional(readOnly = true)
    fun getByMemberId(memberId: Long): MemberSetting? {
        return memberSettingRepository.findById(memberId)
            .map(MemberSetting::from)
            .orElse(null)
    }

    @Transactional
    fun create(
        memberId: Long,
        memberSettingOptions: MemberSettingOptions,
    ): MemberSetting {
        val entity = MemberSettingEntity.create(memberId, memberSettingOptions)
        memberSettingRepository.save(entity)
        return MemberSetting.from(entity)
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

}