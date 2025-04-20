package app.thunder.api.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("sms")
data class SmsProperties(
    val testModeEnabled: Boolean = false,
)