package app.thunder.storage.db.member.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Table(name = "member_fcm_token")
@Entity
internal class MemberFcmTokenEntity private constructor(
    memberId: Long,
    fcmToken: String,
) {
    @Id
    @Column(name = "member_id")
    val memberId: Long = memberId

    @Column(name = "fcm_token", unique = true)
    var fcmToken: String = fcmToken
        protected set

    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now()

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null
        protected set

    companion object {
        fun create(
            memberId: Long,
            fcmToken: String,
        ): MemberFcmTokenEntity {
            return MemberFcmTokenEntity(memberId, fcmToken)
        }
    }

    fun update(fcmToken: String, updatedAt: LocalDateTime?) {
        this.fcmToken = fcmToken
        this.updatedAt = updatedAt
    }

}
