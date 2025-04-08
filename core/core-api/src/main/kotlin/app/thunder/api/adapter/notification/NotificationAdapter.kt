package app.thunder.api.adapter.notification

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.Notification
import app.thunder.domain.notification.NotificationPort
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class NotificationAdapter(
    private val firebaseMessaging: FirebaseMessaging
) : NotificationPort {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun sendNotification(
        fcmToken: String,
        title: String,
        body: String,
        imageUrl: String?,
        routePath: String
    ) {
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

        try {
            firebaseMessaging.send(message)
        } catch (e: FirebaseMessagingException) {
            log.error("FCM failed -- [token={} | {}]", fcmToken, e.message)
        }
    }

}
