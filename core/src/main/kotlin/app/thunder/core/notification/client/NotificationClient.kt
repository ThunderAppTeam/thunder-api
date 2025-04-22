package app.thunder.core.notification.client

import app.thunder.core.notification.domain.NotificationResult

interface NotificationClient {
    fun sendNotification(
        fcmToken: String,
        title: String,
        body: String,
        imageUrl: String? = null,
        routePath: String,
    ): NotificationResult
}
