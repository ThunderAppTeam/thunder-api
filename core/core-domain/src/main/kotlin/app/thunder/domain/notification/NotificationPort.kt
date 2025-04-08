package app.thunder.domain.notification

interface NotificationPort {
    fun sendNotification(
        fcmToken: String,
        title: String,
        body: String,
        imageUrl: String? = null,
        routePath: String,
    )
}
