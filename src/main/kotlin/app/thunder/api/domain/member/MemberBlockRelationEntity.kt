package app.thunder.api.domain.member

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Table(name = "member_block_relation")
@Entity
class MemberBlockRelationEntity private constructor(
    memberId: Long,
    blockedMemberId: Long,
    createdBy: Long,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_block_relation_id")
    val memberBlockRelationId: Long = 0

    @Column(name = "member_id", nullable = false)
    val memberId: Long = memberId

    @Column(name = "blocked_member_id", nullable = false)
    val blockedMemberId: Long = blockedMemberId

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()

    @Column(name = "created_by", nullable = false)
    val createdBy: Long = createdBy


    companion object {
        fun create(
            memberId: Long,
            blockedMemberId: Long,
            createdBy: Long,
        ): MemberBlockRelationEntity {
            return MemberBlockRelationEntity(
                memberId = memberId,
                blockedMemberId = blockedMemberId,
                createdBy = createdBy,
            )
        }
    }

}
