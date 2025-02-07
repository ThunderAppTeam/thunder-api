package app.thunder.api.application

import app.thunder.api.adapter.storage.StorageAdapter
import app.thunder.api.controller.response.GetBodyPhotoResponse
import app.thunder.api.controller.response.GetBodyPhotoResultResponse
import app.thunder.api.domain.body.ReviewableBodyPhotoAdapter
import app.thunder.api.domain.member.MemberAdapter
import app.thunder.api.domain.photo.BodyPhoto
import app.thunder.api.domain.photo.BodyPhotoAdapter
import app.thunder.api.exception.BodyErrors.UNSUPPORTED_IMAGE_FORMAT
import app.thunder.api.exception.BodyErrors.UPLOADER_OR_ADMIN_ONLY_ACCESS
import app.thunder.api.exception.ThunderException
import app.thunder.api.func.toKoreaZonedDateTime
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.util.*
import kotlin.math.round

@Service
class BodyPhotoService(
    private val memberAdapter: MemberAdapter,
    private val bodyPhotoAdapter: BodyPhotoAdapter,
    private val storageAdapter: StorageAdapter,
    private val reviewableBodyPhotoAdapter: ReviewableBodyPhotoAdapter,
) {

    @Transactional(readOnly = true)
    fun getAllByMemberId(memberId: Long): List<GetBodyPhotoResponse> {
        return bodyPhotoAdapter.getAllByMemberId(memberId).map {
            GetBodyPhotoResponse(
                bodyPhotoId = it.bodyPhotoId,
                imageUrl = it.imageUrl,
                isReviewCompleted = it.isReviewCompleted(),
                reviewCount = it.reviewCount,
                reviewScore = it.getResultReviewScore(),
                createdAt = it.createdAt.toKoreaZonedDateTime()
            )
        }
    }

    @Transactional(readOnly = true)
    fun getByBodyPhotoId(bodyPhotoId: Long, memberId: Long): GetBodyPhotoResultResponse {
        val bodyPhoto = bodyPhotoAdapter.getById(bodyPhotoId)

        val member = memberAdapter.getById(memberId)
        val bodyPhotos = bodyPhotoAdapter.getAllByGender(member.gender)
        var ranking = 1.0
        for (other in bodyPhotos) {
            if (other.getResultReviewScore() > bodyPhoto.getResultReviewScore()) {
                ranking += 1
            }
        }
        val genderTopPercent = ranking / bodyPhotos.size * 100

        return GetBodyPhotoResultResponse(
            bodyPhotoId = bodyPhoto.bodyPhotoId,
            imageUrl = bodyPhoto.imageUrl,
            isReviewCompleted = bodyPhoto.isReviewCompleted(),
            reviewCount = bodyPhoto.reviewCount,
            progressRate = bodyPhoto.progressRate(),
            gender = member.gender,
            reviewScore = bodyPhoto.getResultReviewScore(),
            genderTopRate = round(genderTopPercent),
            createdAt = bodyPhoto.createdAt.toKoreaZonedDateTime(),
        )
    }

    @Transactional
    fun upload(imageFile: MultipartFile, memberId: Long): BodyPhoto {
        val isNotAllowImage = !setOf(MediaType.IMAGE_PNG_VALUE, MediaType.IMAGE_JPEG_VALUE)
            .contains(imageFile.contentType)
        if (isNotAllowImage) {
            throw ThunderException(UNSUPPORTED_IMAGE_FORMAT)
        }

        val member = memberAdapter.getById(memberId)
        val fileName = "${UUID.randomUUID()}_${imageFile.originalFilename}"
        val filePath = "${member.nickname}/$BODY_PHOTO_PATH/$fileName"
        val imageUrl = storageAdapter.upload(imageFile, filePath)

        val bodyPhoto = bodyPhotoAdapter.create(memberId, imageUrl)
        return bodyPhoto
    }

    @Transactional
    fun deleteByBodyPhotoId(bodyPhotoId: Long, memberId: Long) {
        val bodyPhoto = bodyPhotoAdapter.getById(bodyPhotoId)
        if (bodyPhoto.isNotUploader(memberId)) {
            throw ThunderException(UPLOADER_OR_ADMIN_ONLY_ACCESS)
        }
        bodyPhotoAdapter.deleteById(bodyPhotoId)
        reviewableBodyPhotoAdapter.deleteByBodyPhotoId(bodyPhotoId)
    }

    companion object {
        private const val BODY_PHOTO_PATH = "body_photo"
    }

}