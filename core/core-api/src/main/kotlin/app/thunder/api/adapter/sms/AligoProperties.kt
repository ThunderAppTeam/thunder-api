package app.thunder.api.adapter.sms

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("aligo")
data class AligoProperties(
    val userId: String,
    val apiKey: String,
    val sender: String,
)