package app.thunder.api.application

import app.thunder.api.adapter.storage.StorageAdapter
import app.thunder.api.domain.body.BodyPhotoEntity
import app.thunder.api.domain.body.BodyPhotoRepository
import app.thunder.api.domain.member.MemberRepository
import app.thunder.api.exception.BodyErrors.UNSUPPORTED_IMAGE_FORMAT
import app.thunder.api.exception.MemberErrors.NOT_FOUND_MEMBER
import app.thunder.api.exception.ThunderException
import org.springframework.http.MediaType
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.util.*

@Service
class BodyService(
    private val memberRepository: MemberRepository,
    private val bodyPhotoRepository: BodyPhotoRepository,
    private val storageAdapter: StorageAdapter,
) {

    companion object {
        private const val BODY_PHOTO_PATH = "body_photo"
    }

    fun upload(imageFile: MultipartFile): String {
        val isNotAllowImage = !setOf(MediaType.IMAGE_PNG_VALUE, MediaType.IMAGE_JPEG_VALUE)
            .contains(imageFile.contentType)
        if (isNotAllowImage) {
            throw ThunderException(UNSUPPORTED_IMAGE_FORMAT)
        }

        val memberId = SecurityContextHolder.getContext().authentication.principal as Long
        val memberEntity = memberRepository.findById(memberId)
            .orElseThrow { ThunderException(NOT_FOUND_MEMBER) }

        val fileName = "${UUID.randomUUID()}_${imageFile.originalFilename}"
        val filePath = "${memberEntity.mobileNumber}/$BODY_PHOTO_PATH/$fileName"
        val imageUrl = storageAdapter.upload(imageFile, filePath)

        val bodyPhotoEntity = BodyPhotoEntity.create(memberId, imageUrl)
        bodyPhotoRepository.save(bodyPhotoEntity)
        return imageUrl
    }

}