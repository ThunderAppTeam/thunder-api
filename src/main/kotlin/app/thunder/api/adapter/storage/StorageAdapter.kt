package app.thunder.api.adapter.storage

import io.awspring.cloud.s3.ObjectMetadata
import io.awspring.cloud.s3.S3Operations
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile

@Component
class StorageAdapter(
    private val s3Operations: S3Operations,
    private val s3Properties: AwsS3Properties,
) {

    @Transactional
    fun upload(file: MultipartFile, filePath: String): String {
        val metadata = ObjectMetadata.builder().contentType(file.contentType).build()
        val s3Resource = s3Operations.upload(s3Properties.bucket, filePath, file.inputStream, metadata)
        return s3Resource.url.toExternalForm()
    }

}