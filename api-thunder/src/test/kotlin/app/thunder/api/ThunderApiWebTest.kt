package app.thunder.api

import app.thunder.api.auth.TokenManager
import app.thunder.api.scheduler.NotificationScheduler
import com.google.firebase.messaging.FirebaseMessaging
import java.time.LocalDateTime
import java.util.UUID
import org.junit.jupiter.api.AfterEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc

@SpringBootTest(
    properties = [
        "AWS_ACCESS_KEY=test-access-key",
        "AWS_SECRET_KEY=test-secret-key",
        "AWS_S3_BUCKET=test-bucket",
        "AWS_REGION=ap-northeast-2",
        "JWT_SECRET_KEY=test-jwt-secret-key-with-at-least-32-bytes",
        "JWT_ACCESS_TOKEN_EXPIRATION=3600000",
        "JWT_REFRESH_TOKEN_EXPIRATION=86400000",
        "FIREBASE_ACCOUNT_PATH=unused",
        "ALIGO_USER_ID=test-user",
        "ALIGO_API_KEY=test-api-key",
        "ALIGO_SENDER=01000000000",
        "ADMIN_MEMBER_IDS=900001",
    ],
)
@AutoConfigureMockMvc
@Import(PostgresContainerConfiguration::class)
abstract class ThunderApiWebTest {

    @MockitoBean
    protected lateinit var firebaseMessaging: FirebaseMessaging

    @MockitoBean
    private lateinit var cronDrivenNotificationScheduler: NotificationScheduler

    @Autowired
    protected lateinit var mockMvc: MockMvc

    @Autowired
    protected lateinit var tokenManager: TokenManager

    @Autowired
    protected lateinit var jdbcTemplate: JdbcTemplate

    @AfterEach
    fun deleteFixtures() {
        listOf("member_setting", "member", "mobile_verification", "release_ui")
            .forEach { jdbcTemplate.update("DELETE FROM $it") }
    }

    protected fun bearerToken(memberId: Long): String =
        "Bearer ${tokenManager.generateAccessToken(memberId)}"

    protected fun insertMember(memberId: Long, loggedOutAt: LocalDateTime? = null) {
        jdbcTemplate.update(
            """
            INSERT INTO member (member_id, nickname, mobile_number, mobile_country, gender, birth_day,
                                country_code, marketing_agreement, member_uuid, logged_out_at, created_at)
            OVERRIDING SYSTEM VALUE
            VALUES (?, ?, ?, 'KR', 'MALE', DATE '1990-01-01', 'KR', FALSE, ?, ?, ?)
            """.trimIndent(),
            memberId, "user$memberId", "010$memberId", UUID.randomUUID(), loggedOutAt, LocalDateTime.now(),
        )
    }

    protected fun insertVerification(
        deviceId: String,
        mobileNumber: String,
        verificationCode: String = "123456",
        createdAt: LocalDateTime = LocalDateTime.now(),
        verifiedAt: LocalDateTime? = null,
        failedAttemptCount: Int = 0,
    ) {
        jdbcTemplate.update(
            """
            INSERT INTO mobile_verification (device_id, mobile_number, mobile_country, verification_code,
                                             verified_at, created_at, expired_at, failed_attempt_count)
            VALUES (?, ?, 'KR', ?, ?, ?, ?, ?)
            """.trimIndent(),
            deviceId, mobileNumber, verificationCode, verifiedAt, createdAt, createdAt.plusMinutes(3),
            failedAttemptCount,
        )
    }

    protected fun countVerifications(where: String, vararg args: Any): Int =
        jdbcTemplate.queryForObject("SELECT COUNT(*) FROM mobile_verification WHERE $where", Int::class.java, *args)!!

    companion object {
        const val ADMIN_MEMBER_ID = 900_001L
    }

}
