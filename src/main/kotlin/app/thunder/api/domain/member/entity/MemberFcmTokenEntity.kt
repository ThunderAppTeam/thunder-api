package app.thunder.api.domain.member.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Table(name = "member_fcm_token")
@Entity
class MemberFcmTokenEntity private constructor(
    memberId: Long,
    fcmToken: String,
) {
    @Id
    @Column(name = "member_id")
    val memberId: Long = memberId

    @Column(name = "fcm_token", unique = true)
    val fcmToken: String = fcmToken

    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now()

    companion object {
        fun create(
            memberId: Long,
            fcmToken: String,
        ): MemberFcmTokenEntity {
            return MemberFcmTokenEntity(memberId, fcmToken)
        }
    }

}
