package app.thunder.api.domain.member

import app.thunder.api.domain.member.entity.MemberSettingEntity
import java.time.LocalDateTime

class MemberSetting private constructor(
    val memberId: Long,
    reviewCompleteNotify: Boolean,
    reviewRequestNotify: Boolean,
    marketingAgreement: Boolean,
    val createdAt: LocalDateTime,
    updatedAt: LocalDateTime?,
) {

    var reviewCompleteNotify: Boolean = reviewCompleteNotify
        private set
    var reviewRequestNotify: Boolean = reviewRequestNotify
        private set
    var marketingAgreement: Boolean = marketingAgreement
        private set
    var updatedAt: LocalDateTime? = updatedAt
        private set


    companion object {
        fun from(entity: MemberSettingEntity): MemberSetting {
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

    fun setOptions(options: MemberSettingOptions) {
        this.reviewRequestNotify = options.reviewRequestNotify
        this.reviewCompleteNotify = options.reviewCompleteNotify
        this.marketingAgreement = options.marketingAgreement
        this.updatedAt = LocalDateTime.now()
    }

}
