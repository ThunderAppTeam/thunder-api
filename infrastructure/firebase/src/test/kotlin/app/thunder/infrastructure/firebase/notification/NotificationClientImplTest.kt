package app.thunder.infrastructure.firebase.notification

import app.thunder.core.notification.domain.NotificationResult
import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.MessagingErrorCode
import org.assertj.core.api.Assertions.assertThat
import org.instancio.Instancio
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.slf4j.LoggerFactory

class NotificationClientImplTest {

    private lateinit var firebaseMessaging: FirebaseMessaging
    private lateinit var client: NotificationClientImpl
    private lateinit var logAppender: ListAppender<ILoggingEvent>
    private val logger = LoggerFactory.getLogger(NotificationClientImpl::class.java) as Logger

    @BeforeEach
    fun setUp() {
        firebaseMessaging = mock(FirebaseMessaging::class.java)
        client = NotificationClientImpl(firebaseMessaging)
        logAppender = ListAppender<ILoggingEvent>().apply { start() }
        logger.addAppender(logAppender)
        logger.level = Level.ALL
    }

    @AfterEach
    fun tearDown() {
        logger.detachAppender(logAppender)
    }

    @Test
    @DisplayName("FCM이 전송을 받아들이면 SENT를 돌려준다")
    fun sent() {
        // given
        val token = fcmToken()

        // when
        val result = send(token)

        // then
        assertThat(result).isEqualTo(NotificationResult.SENT)
    }

    @ParameterizedTest
    @EnumSource(value = MessagingErrorCode::class, names = ["UNREGISTERED", "INVALID_ARGUMENT"])
    @DisplayName("FCM이 토큰 오류로 거절하면 INVALID_TOKEN을 돌려준다")
    fun invalidToken(errorCode: MessagingErrorCode) {
        // given
        val token = fcmToken()
        failWith(errorCode)

        // when
        val result = send(token)

        // then
        assertThat(result).isEqualTo(NotificationResult.INVALID_TOKEN)
    }

    @ParameterizedTest
    @EnumSource(value = MessagingErrorCode::class, names = ["UNREGISTERED", "INVALID_ARGUMENT"], mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("토큰과 무관한 FCM 오류는 FAILED를 돌려준다")
    fun otherFailure(errorCode: MessagingErrorCode) {
        // given
        val token = fcmToken()
        failWith(errorCode)

        // when
        val result = send(token)

        // then
        assertThat(result).isEqualTo(NotificationResult.FAILED)
    }

    @Test
    @DisplayName("오류 코드가 없는 FCM 예외는 FAILED를 돌려준다")
    fun failureWithoutErrorCode() {
        // given
        val token = fcmToken()
        failWith(null)

        // when
        val result = send(token)

        // then
        assertThat(result).isEqualTo(NotificationResult.FAILED)
    }

    @Test
    @DisplayName("전송에 실패해도 로그에는 FCM 토큰 앞 6자와 말줄임표만 남긴다")
    fun logsMaskedToken() {
        // given
        val token = fcmToken()
        failWith(MessagingErrorCode.UNREGISTERED)

        // when
        send(token)

        // then
        val messages = logAppender.list.map { it.formattedMessage }
        assertThat(messages).isNotEmpty()
        assertThat(messages).allSatisfy { assertThat(it).doesNotContain(token) }
        assertThat(messages.joinToString()).contains("${token.take(6)}...")
    }

    private fun send(token: String): NotificationResult = client.sendNotification(
        fcmToken = token,
        title = "title",
        body = "body",
        imageUrl = null,
        routePath = "/",
    )

    private fun failWith(errorCode: MessagingErrorCode?) {
        val exception = mock(FirebaseMessagingException::class.java)
        `when`(exception.messagingErrorCode).thenReturn(errorCode)
        `when`(exception.message).thenReturn("fcm failure")
        doThrow(exception).`when`(firebaseMessaging).send(org.mockito.ArgumentMatchers.any(Message::class.java))
    }

    private fun fcmToken(): String = Instancio.gen().string().alphaNumeric().length(40).get()

}
