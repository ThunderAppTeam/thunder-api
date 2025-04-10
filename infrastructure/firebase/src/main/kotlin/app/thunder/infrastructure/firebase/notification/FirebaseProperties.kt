package app.thunder.infrastructure.firebase.notification

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("firebase")
data class FirebaseProperties(
    val accountPath: String,
)
