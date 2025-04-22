package app.thunder.api.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("admin")
data class AdminProperties(
    val memberIds: List<Long> = emptyList(),
)
