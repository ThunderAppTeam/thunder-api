package app.thunder.infrastructure.postgresql.member.persistence

import app.thunder.infrastructure.postgresql.member.entity.MemberEntity
import java.time.LocalDateTime
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

internal interface MemberJpaRepository : JpaRepository<MemberEntity, Long> {
    fun findByMobileNumber(mobileNumber: String): MemberEntity?
    fun findByNickname(nickName: String): MemberEntity?

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
        """
        UPDATE MemberEntity m
        SET m.loggedOutAt = :revokedAt,
            m.updatedAt = :revokedAt,
            m.updatedBy = :memberId
        WHERE m.memberId = :memberId AND (m.loggedOutAt IS NULL OR m.loggedOutAt < :revokedAt)
        """
    )
    fun revokeTokensIssuedBefore(
        @Param("memberId") memberId: Long,
        @Param("revokedAt") revokedAt: LocalDateTime,
    ): Int
}
