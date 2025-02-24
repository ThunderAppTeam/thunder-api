package app.thunder.api.domain.member.adapter

import app.thunder.api.domain.member.repository.MemberFcmTokenRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class FcmTokenAdapter(
    private val memberFcmTokenRepository: MemberFcmTokenRepository,
) {

    @Transactional(readOnly = true)
    fun getByMemberId(memberId: Long): String? {
        return memberFcmTokenRepository.findByMemberId(memberId)?.fcmToken
    }

    @Transactional(readOnly = true)
    fun getMemberIdToFcmTokenMap(memberIds: Collection<Long>): Map<Long, String> {
        return memberFcmTokenRepository.findAllById(memberIds)
            .associate { it.memberId to it.fcmToken }
    }

    @Transactional
    fun createOrUpdate(memberId: Long, fcmToken: String) {
        memberFcmTokenRepository.upsertFcmToken(memberId, fcmToken)
    }

}