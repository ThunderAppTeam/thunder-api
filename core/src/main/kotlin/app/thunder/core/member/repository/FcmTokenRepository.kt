package app.thunder.core.member.repository

interface FcmTokenRepository {
    fun getByMemberId(memberId: Long): String?
    fun getMemberIdToFcmTokenMap(memberIds: Collection<Long>): Map<Long, String>
    fun createOrUpdate(memberId: Long, fcmToken: String)
    fun deleteByMemberId(memberId: Long)
    fun deleteByMemberIdAndFcmToken(memberId: Long, fcmToken: String)
}
