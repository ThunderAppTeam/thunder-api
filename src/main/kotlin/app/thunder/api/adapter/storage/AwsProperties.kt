package app.thunder.api.adapter.storage

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "aws")
data class AwsProperties(
    val credentials: AwsCredentialsProperties,
    val s3: AwsS3Properties,
    val region: AwsRegionProperties,
) {

    data class AwsCredentialsProperties(val accessKey: String, val secretKey: String)

    data class AwsS3Properties(val bucket: String)

    data class AwsRegionProperties(val static: String)

}