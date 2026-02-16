package app.thunder.domain.member

interface MemberSettingPort {
    fun getByMemberId(memberId: Long): MemberSetting?
    fun create(memberId: Long, memberSettingOptions: MemberSettingOptions): MemberSetting
    fun update(memberSetting: MemberSetting)
}