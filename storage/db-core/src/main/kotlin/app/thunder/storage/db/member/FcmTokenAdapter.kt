package app.thunder.storage.db.member

import app.thunder.domain.member.FcmTokenPort
import app.thunder.storage.db.member.persistence.MemberFcmTokenJpaRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
internal class FcmTokenAdapter(
    private val memberFcmTokenJpaRepository: MemberFcmTokenJpaRepository,
) : FcmTokenPort {

    @Transactional(readOnly = true)
    override fun getByMemberId(memberId: Long): String? {
        return memberFcmTokenJpaRepository.findByMemberId(memberId)?.fcmToken
    }

    @Transactional(readOnly = true)
    override fun getMemberIdToFcmTokenMap(memberIds: Collection<Long>): Map<Long, String> {
        return memberFcmTokenJpaRepository.findAllById(memberIds)
            .associate { it.memberId to it.fcmToken }
    }

    @Transactional
    override fun createOrUpdate(memberId: Long, fcmToken: String) {
        memberFcmTokenJpaRepository.upsertFcmToken(memberId, fcmToken)
    }

}
