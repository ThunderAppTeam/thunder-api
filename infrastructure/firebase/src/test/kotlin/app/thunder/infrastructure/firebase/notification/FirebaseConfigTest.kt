package app.thunder.infrastructure.firebase.notification

import com.google.auth.oauth2.ServiceAccountCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.ImplFirebaseTrampolines
import com.google.firebase.messaging.FirebaseMessaging
import java.io.FileNotFoundException
import java.io.IOException
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Path
import java.security.KeyPairGenerator
import java.util.Base64
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.instancio.Instancio
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class FirebaseConfigTest {

    @TempDir
    lateinit var resourceDir: Path

    private lateinit var originalClassLoader: ClassLoader
    private val config = FirebaseConfig()

    @BeforeEach
    fun setUp() {
        deleteFirebaseApps()
        originalClassLoader = Thread.currentThread().contextClassLoader
        Thread.currentThread().contextClassLoader = URLClassLoader(arrayOf(resourceDir.toUri().toURL()), originalClassLoader)
    }

    @AfterEach
    fun tearDown() {
        Thread.currentThread().contextClassLoader = originalClassLoader
        deleteFirebaseApps()
    }

    @Test
    @DisplayName("클래스패스의 서비스 계정 파일로 인증한 FirebaseMessaging을 만든다")
    fun createsFirebaseMessagingFromServiceAccount() {
        // given
        val clientEmail = "${Instancio.create(String::class.java).lowercase()}@thunder-test.iam.gserviceaccount.com"
        writeServiceAccount(SERVICE_ACCOUNT_FILE, clientEmail)

        // when
        val messaging = config.firebaseMessaging(FirebaseProperties(SERVICE_ACCOUNT_FILE))

        // then
        val app = FirebaseApp.getInstance()
        val credentials = ImplFirebaseTrampolines.getCredentials(app) as ServiceAccountCredentials
        assertThat(credentials.clientEmail).isEqualTo(clientEmail)
        assertThat(messaging).isSameAs(FirebaseMessaging.getInstance(app))
    }

    @Test
    @DisplayName("서비스 계정 파일이 클래스패스에 없으면 FileNotFoundException을 던진다")
    fun failsWhenAccountFileIsMissing() {
        // given
        val properties = FirebaseProperties("missing-${Instancio.create(String::class.java)}.json")

        // when
        val thrown = catchThrowable { config.firebaseMessaging(properties) }

        // then
        assertThat(thrown).isInstanceOf(FileNotFoundException::class.java)
        assertThat(FirebaseApp.getApps()).isEmpty()
    }

    @Test
    @DisplayName("서비스 계정 파일이 올바른 JSON 자격 증명이 아니면 IOException을 던진다")
    fun failsWhenAccountFileIsNotCredentials() {
        // given
        Files.writeString(resourceDir.resolve(SERVICE_ACCOUNT_FILE), "{}")

        // when
        val thrown = catchThrowable { config.firebaseMessaging(FirebaseProperties(SERVICE_ACCOUNT_FILE)) }

        // then
        assertThat(thrown).isInstanceOf(IOException::class.java)
        assertThat(FirebaseApp.getApps()).isEmpty()
    }

    private fun writeServiceAccount(fileName: String, clientEmail: String) {
        val privateKey = KeyPairGenerator.getInstance("RSA")
            .apply { initialize(RSA_KEY_SIZE) }
            .generateKeyPair()
            .private
        val body = Base64.getMimeEncoder(PEM_LINE_LENGTH, "\n".toByteArray()).encodeToString(privateKey.encoded)
        val pem = "-----BEGIN PRIVATE KEY-----\n$body\n-----END PRIVATE KEY-----\n".replace("\n", "\\n")
        val json = """
            {
              "type": "service_account",
              "project_id": "thunder-test",
              "private_key_id": "test-key-id",
              "private_key": "$pem",
              "client_email": "$clientEmail",
              "client_id": "1234567890"
            }
        """.trimIndent()
        Files.writeString(resourceDir.resolve(fileName), json)
    }

    private fun deleteFirebaseApps() {
        FirebaseApp.getApps().forEach { it.delete() }
    }

    companion object {
        private const val SERVICE_ACCOUNT_FILE = "test-service-account.json"
        private const val RSA_KEY_SIZE = 2048
        private const val PEM_LINE_LENGTH = 64
    }

}
