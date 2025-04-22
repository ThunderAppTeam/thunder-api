package app.thunder.infrastructure.postgresql.member.persistence

import app.thunder.infrastructure.postgresql.member.entity.MemberFcmTokenEntity
import com.linecorp.kotlinjdsl.support.spring.data.jpa.repository.KotlinJdslJpqlExecutor
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

internal interface MemberFcmTokenJpaRepository : JpaRepository<MemberFcmTokenEntity, Long>, KotlinJdslJpqlExecutor {

    fun findByMemberId(memberId: Long): MemberFcmTokenEntity?

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM MemberFcmTokenEntity e WHERE e.fcmToken = :fcmToken AND e.memberId <> :memberId")
    fun deleteByFcmTokenOfOtherMembers(@Param("fcmToken") fcmToken: String, @Param("memberId") memberId: Long): Int

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM MemberFcmTokenEntity e WHERE e.memberId = :memberId AND e.fcmToken = :fcmToken")
    fun deleteByMemberIdAndFcmToken(@Param("memberId") memberId: Long, @Param("fcmToken") fcmToken: String): Int

    @Modifying
    @Query(
        """
        INSERT INTO member_fcm_token (member_id, fcm_token, created_at, updated_at)
        VALUES (:memberId, :fcmToken, NOW(), NULL)
        ON CONFLICT (member_id)
        DO UPDATE SET fcm_token = :fcmToken, updated_at = NOW()
    """,
        nativeQuery = true,
    )
    fun upsertFcmToken(@Param("memberId") memberId: Long, @Param("fcmToken") fcmToken: String): Int

}
