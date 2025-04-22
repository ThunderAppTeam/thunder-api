package app.thunder.infrastructure.firebase.notification

import app.thunder.core.notification.client.NotificationClient
import app.thunder.core.notification.domain.NotificationResult
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.MessagingErrorCode
import com.google.firebase.messaging.Notification
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
internal class NotificationClientImpl(
    private val firebaseMessaging: FirebaseMessaging
) : NotificationClient {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun sendNotification(
        fcmToken: String,
        title: String,
        body: String,
        imageUrl: String?,
        routePath: String
    ): NotificationResult {
        val notification = Notification.builder()
            .setTitle(title)
            .setBody(body)
            .apply { imageUrl?.let { setImage(it) } }
            .build()

        val message = Message.builder()
            .setToken(fcmToken)
            .setNotification(notification)
            .putData("routePath", routePath)
            .build()

        return try {
            firebaseMessaging.send(message)
            NotificationResult.SENT
        } catch (e: FirebaseMessagingException) {
            log.error("FCM failed -- [token={} | {}]", maskToken(fcmToken), e.message)
            if (e.messagingErrorCode in INVALID_TOKEN_ERROR_CODES) NotificationResult.INVALID_TOKEN
            else NotificationResult.FAILED
        }
    }

    private fun maskToken(fcmToken: String): String = fcmToken.take(TOKEN_LOG_LENGTH) + "..."

    companion object {
        private const val TOKEN_LOG_LENGTH = 6
        private val INVALID_TOKEN_ERROR_CODES = setOf(
            MessagingErrorCode.UNREGISTERED,
            MessagingErrorCode.INVALID_ARGUMENT,
        )
    }

}
