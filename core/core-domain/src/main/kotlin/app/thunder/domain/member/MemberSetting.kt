package app.thunder.domain.member

import java.time.LocalDateTime

class MemberSetting constructor(
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


    fun setOptions(options: MemberSettingOptions) {
        this.reviewRequestNotify = options.reviewRequestNotify
        this.reviewCompleteNotify = options.reviewCompleteNotify
        this.marketingAgreement = options.marketingAgreement
        this.updatedAt = LocalDateTime.now()
    }

}
