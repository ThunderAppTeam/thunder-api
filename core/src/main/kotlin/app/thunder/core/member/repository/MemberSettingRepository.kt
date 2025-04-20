package app.thunder.core.member.repository

import app.thunder.core.member.domain.MemberSetting
import app.thunder.core.member.domain.MemberSettingOptions

interface MemberSettingRepository {
    fun getByMemberId(memberId: Long): MemberSetting?
    fun create(memberId: Long, memberSettingOptions: MemberSettingOptions): MemberSetting
    fun update(memberSetting: MemberSetting)
}