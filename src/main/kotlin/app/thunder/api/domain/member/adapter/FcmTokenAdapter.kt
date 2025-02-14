package app.thunder.api.domain.member.adapter

import app.thunder.api.domain.member.entity.MemberFcmTokenEntity
import app.thunder.api.domain.member.repository.MemberFcmTokenRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class FcmTokenAdapter(
    private val memberFcmTokenRepository: MemberFcmTokenRepository,
) {

    @Transactional(readOnly =true)
    fun existsByMemberId(memberId:Long): Boolean {
        return memberFcmTokenRepository.existsById(memberId)
    }

    @Transactional
    fun create(
        memberId: Long,
        fcmToken: String
    ) {
        val entity = MemberFcmTokenEntity.create(memberId, fcmToken)
        memberFcmTokenRepository.save(entity)
    }

}