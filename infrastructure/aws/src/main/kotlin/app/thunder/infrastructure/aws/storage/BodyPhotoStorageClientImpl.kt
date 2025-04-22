package app.thunder.infrastructure.aws.storage

import app.thunder.core.photo.client.BodyPhotoStorageClient
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest

@Component
internal class BodyPhotoStorageClientImpl(
    private val s3Client: S3Client,
    private val awsProperties: AwsProperties,
) : BodyPhotoStorageClient {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun upload(file: ByteArray, contentType: String?, filePath: String): String {
        val putObjectRequest = PutObjectRequest.builder()
            .bucket(awsProperties.s3.bucket)
            .key(filePath)
            .contentType(contentType ?: MediaType.IMAGE_JPEG_VALUE)
            .build()
        val requestBody = RequestBody.fromInputStream(file.inputStream(), file.size.toLong())
        s3Client.putObject(putObjectRequest, requestBody)

        return "${urlPrefix()}$filePath"
    }

    override fun delete(imageUrl: String) {
        val prefix = urlPrefix()
        if (!imageUrl.startsWith(prefix)) {
            log.warn("Skip deleting body photo: image url is not in the bucket [imageUrl: {}]", imageUrl)
            return
        }
        val deleteObjectRequest = DeleteObjectRequest.builder()
            .bucket(awsProperties.s3.bucket)
            .key(imageUrl.removePrefix(prefix))
            .build()
        s3Client.deleteObject(deleteObjectRequest)
    }

    private fun urlPrefix(): String {
        return "https://${awsProperties.s3.bucket}.s3.${awsProperties.region.static}.amazonaws.com/"
    }

}
