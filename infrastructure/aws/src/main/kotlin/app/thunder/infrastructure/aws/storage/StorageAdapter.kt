package app.thunder.infrastructure.aws.storage

import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest

@Component
class StorageAdapter(
    private val s3Client: S3Client,
    private val awsProperties: AwsProperties,
) {

    fun upload(file: ByteArray, contentType: String?, filePath: String): String {
        val bucket = awsProperties.s3.bucket
        val region = awsProperties.region.static

        val putObjectRequest = PutObjectRequest.builder()
            .bucket(bucket)
            .key(filePath)
            .contentType(contentType ?: MediaType.IMAGE_JPEG_VALUE)
            .build()
        val requestBody = RequestBody.fromInputStream(file.inputStream(), file.size.toLong())
        s3Client.putObject(putObjectRequest, requestBody)

        val imageUrl = "https://$bucket.s3.$region.amazonaws.com/$filePath"
        return imageUrl
    }

    fun delete(imageUrl: String) {
        val deleteObjectRequest = DeleteObjectRequest.builder()
            .bucket(awsProperties.s3.bucket)
            .key(imageUrl)
            .build()
        s3Client.deleteObject(deleteObjectRequest)
    }

}
