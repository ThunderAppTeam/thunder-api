package app.thunder.api.adapter.notification

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.Notification
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class NotificationAdapter(
    private val firebaseMessaging: FirebaseMessaging
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun sendMulticastNotification(
        token: String,
        title: String,
        body: String,
        imageUrl: String? = null,
        routePath: String? = null
    ) {
        val notification = Notification.builder()
            .apply {
                this.setTitle(title)
                this.setBody(body)
                imageUrl?.let { this.setImage(imageUrl) }
            }
            .build()

        val message = Message.builder()
            .apply {
                this.setToken(token)
                this.setNotification(notification)
                routePath?.let { this.putData("routePath", routePath) }
            }
            .build()

        try {
            firebaseMessaging.send(message)
        } catch (e: FirebaseMessagingException) {
            log.error("FCM failed -- [token={} | {}]", token, e.message)
        }
    }

}