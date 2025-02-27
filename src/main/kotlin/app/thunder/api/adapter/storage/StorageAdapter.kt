package app.thunder.api.adapter.storage

import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest

@Component
class StorageAdapter(
    private val s3Client: S3Client,
    private val awsProperties: AwsProperties,
) {

    fun upload(file: MultipartFile, filePath: String): String {
        val bucket = awsProperties.s3.bucket
        val region = awsProperties.region.static

        val putObjectRequest = PutObjectRequest.builder()
            .bucket(bucket)
            .key(filePath)
            .contentType(file.contentType)
            .build()
        val requestBody = RequestBody.fromInputStream(file.inputStream, file.size)
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