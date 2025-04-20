package app.thunder.api

import com.google.firebase.messaging.FirebaseMessaging
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.ApplicationContext
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
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
    ],
)
class ThunderApiApplicationTest {

    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:16-alpine")
    }

    @MockitoBean
    lateinit var firebaseMessaging: FirebaseMessaging

    @Autowired
    lateinit var applicationContext: ApplicationContext

    @Test
    @DisplayName("빈 PostgreSQL에 migration을 적용한 뒤 모든 Bean이 연결된 상태로 애플리케이션이 기동한다")
    fun contextLoads() {
        // given
        val applicationClass = ThunderApiApplication::class.java

        // when
        val application = applicationContext.getBean(applicationClass)

        // then
        assertThat(application).isNotNull
    }

}
