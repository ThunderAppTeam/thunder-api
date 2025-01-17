package app.thunder.api.application

import app.thunder.api.adapter.storage.StorageAdapter
import app.thunder.api.domain.body.BodyPhotoEntity
import app.thunder.api.domain.body.BodyPhotoRepository
import app.thunder.api.domain.body.BodyReviewAdapter
import app.thunder.api.domain.member.MemberAdapter
import app.thunder.api.exception.BodyErrors.UNSUPPORTED_IMAGE_FORMAT
import app.thunder.api.exception.ThunderException
import java.util.UUID
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile

@Service
class BodyService(
    private val bodyPhotoRepository: BodyPhotoRepository,
    private val memberAdapter: MemberAdapter,
    private val bodyReviewAdapter: BodyReviewAdapter,
    private val storageAdapter: StorageAdapter,
) {

    companion object {
        private const val BODY_PHOTO_PATH = "body_photo"
    }

    @Transactional
    fun upload(imageFile: MultipartFile, memberId: Long): String {
        val isNotAllowImage = !setOf(MediaType.IMAGE_PNG_VALUE, MediaType.IMAGE_JPEG_VALUE)
            .contains(imageFile.contentType)
        if (isNotAllowImage) {
            throw ThunderException(UNSUPPORTED_IMAGE_FORMAT)
        }

        val member = memberAdapter.getByMemberId(memberId)
        val fileName = "${UUID.randomUUID()}_${imageFile.originalFilename}"
        val filePath = "${member.nickname}/$BODY_PHOTO_PATH/$fileName"
        val imageUrl = storageAdapter.upload(imageFile, filePath)

        val bodyPhotoEntity = BodyPhotoEntity.create(memberId, imageUrl)
        bodyPhotoRepository.save(bodyPhotoEntity)

        return imageUrl
    }

    @Transactional
    fun review(bodyPhotoId: Long, memberId: Long, score: Int) {
        val member = memberAdapter.getByMemberId(memberId)
        bodyReviewAdapter.create(bodyPhotoId, member.memberId, score)
    }

}