package app.thunder.domain.member

interface FcmTokenPort {
    fun getByMemberId(memberId: Long): String?
    fun getMemberIdToFcmTokenMap(memberIds: Collection<Long>): Map<Long, String>
    fun createOrUpdate(memberId: Long, fcmToken: String)
}
