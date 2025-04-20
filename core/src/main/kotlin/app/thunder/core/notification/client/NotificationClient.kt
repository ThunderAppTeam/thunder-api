package app.thunder.core.notification.client

interface NotificationClient {
    fun sendNotification(
        fcmToken: String,
        title: String,
        body: String,
        imageUrl: String? = null,
        routePath: String,
    )
}
