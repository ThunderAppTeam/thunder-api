package app.thunder.infrastructure.db.member.persistence

import app.thunder.infrastructure.db.member.entity.MemberFcmTokenEntity
import com.linecorp.kotlinjdsl.support.spring.data.jpa.repository.KotlinJdslJpqlExecutor
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

internal interface MemberFcmTokenJpaRepository : JpaRepository<MemberFcmTokenEntity, Long>, KotlinJdslJpqlExecutor {

    fun findByMemberId(memberId: Long): MemberFcmTokenEntity?

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
