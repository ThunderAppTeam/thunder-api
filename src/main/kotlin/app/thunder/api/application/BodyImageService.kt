package app.thunder.api.application

import app.thunder.api.adapter.image.ImageAdapter
import app.thunder.api.domain.bodyimage.BodyImageRepository
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile

@Service
class BodyImageService(
    private val bodyImageRepository: BodyImageRepository,
    private val imageAdapter: ImageAdapter,
) {

    fun share(imageFile: MultipartFile, memberId: Long) {
        val isNotAllowImage = !setOf(MediaType.IMAGE_PNG_VALUE, MediaType.IMAGE_JPEG_VALUE)
            .contains(imageFile.contentType)
        if (isNotAllowImage) {
            throw RuntimeException("ContentType must be .png or .jpeg")
        }
    }

}