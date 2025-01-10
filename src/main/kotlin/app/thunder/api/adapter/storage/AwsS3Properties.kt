package app.thunder.api.adapter.storage

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "spring.cloud.aws.s3")
data class AwsS3Properties(
    val bucket: String,
)