package app.thunder.api.adapter.image

import io.awspring.cloud.s3.ObjectMetadata
import io.awspring.cloud.s3.S3Operations
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.net.URL

@Component
class ImageAdapter(
    private val s3Operations: S3Operations,
    private val s3Properties: AwsS3Properties,
) {

    @Transactional
    fun upload(multipartFile: MultipartFile, key: String): URL {
        val bucketName = key
        val metadata = ObjectMetadata.builder().contentType(multipartFile.contentType).build()
        val resource = s3Operations.upload(s3Properties.bucket, key, multipartFile.inputStream, metadata)
        return resource.url
    }

}