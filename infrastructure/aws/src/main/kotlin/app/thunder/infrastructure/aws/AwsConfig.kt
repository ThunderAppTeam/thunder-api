package app.thunder.infrastructure.aws

import app.thunder.infrastructure.aws.storage.AwsProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.rekognition.RekognitionClient
import software.amazon.awssdk.services.s3.S3Client


@Configuration
class AwsConfig(
    private val awsProperties: AwsProperties
) {

    @Bean
    fun awsS3Client(): S3Client {
        val region = Region.of(awsProperties.region.static)
        return S3Client.builder()
            .region(region)
            .credentialsProvider(awsCredentialsProvider())
            .build()
    }

    @Bean
    fun awsRekognitionClient(): RekognitionClient? {
        return RekognitionClient.builder()
            .region(Region.of(awsProperties.region.static))
            .credentialsProvider(awsCredentialsProvider())
            .build()
    }

    @Bean
    fun awsCredentialsProvider(): StaticCredentialsProvider {
        val credentials = AwsBasicCredentials.create(
            awsProperties.credentials.accessKey,
            awsProperties.credentials.secretKey,
        )
        return StaticCredentialsProvider.create(credentials)
    }

}
