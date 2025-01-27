package app.thunder.api.application

import app.thunder.api.adapter.storage.StorageAdapter
import app.thunder.api.controller.response.GetBodyPhotoResponse
import app.thunder.api.controller.response.GetBodyPhotoResultResponse
import app.thunder.api.domain.body.BodyPhoto
import app.thunder.api.domain.body.BodyPhotoAdapter
import app.thunder.api.domain.body.BodyReviewAdapter
import app.thunder.api.domain.body.ReviewRotationAdapter
import app.thunder.api.domain.member.MemberAdapter
import app.thunder.api.exception.BodyErrors.UNSUPPORTED_IMAGE_FORMAT
import app.thunder.api.exception.BodyErrors.UPLOADER_OR_ADMIN_ONLY_ACCESS
import app.thunder.api.exception.ThunderException
import app.thunder.api.func.toKoreaZonedDateTime
import java.time.LocalDateTime
import java.util.UUID
import kotlin.math.round
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile

@Service
class BodyPhotoService(
    private val memberAdapter: MemberAdapter,
    private val bodyPhotoAdapter: BodyPhotoAdapter,
    private val bodyReviewAdapter: BodyReviewAdapter,
    private val storageAdapter: StorageAdapter,
    private val reviewRotationAdapter: ReviewRotationAdapter,
) {

    @Transactional(readOnly = true)
    fun getAllByMemberId(memberId: Long): List<GetBodyPhotoResponse> {
        return bodyPhotoAdapter.getAllByMemberId(memberId).map {
            val is24HoursLater = it.createdAt.plusHours(24).isBefore(LocalDateTime.now())
            GetBodyPhotoResponse(
                bodyPhotoId = it.bodyPhotoId,
                imageUrl = it.imageUrl,
                isReviewCompleted = it.isReviewCompleted || is24HoursLater,
                reviewCount = if (it.reviewScore == 0.0) 0 else 1,
                reviewScore = it.reviewScore,
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
            if (other.reviewScore > bodyPhoto.reviewScore) {
                ranking += 1
            }
        }
        val topPercent = ranking / bodyPhotos.size * 100
        val reviewCount = bodyReviewAdapter.getAllByBodyPhotoId(bodyPhotoId).count()
        val is24HoursLater = bodyPhoto.createdAt.plusHours(24).isBefore(LocalDateTime.now())

        return GetBodyPhotoResultResponse(
            bodyPhotoId = bodyPhoto.bodyPhotoId,
            imageUrl = bodyPhoto.imageUrl,
            isReviewCompleted = bodyPhoto.isReviewCompleted || is24HoursLater,
            reviewCount = reviewCount,
            progressRate = reviewCount / 20 * 100.0,
            gender = member.gender,
            reviewScore = round(bodyPhoto.reviewScore * 10) / 10,
            genderTopRate = round(topPercent),
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
        reviewRotationAdapter.create(bodyPhoto.bodyPhotoId, memberId)
        return bodyPhoto
    }

    @Transactional
    fun deleteByBodyPhotoId(bodyPhotoId: Long, memberId: Long) {
        val bodyPhoto = bodyPhotoAdapter.getById(bodyPhotoId)
        if (bodyPhoto.isNotUploader(memberId)) {
            throw ThunderException(UPLOADER_OR_ADMIN_ONLY_ACCESS)
        }
        bodyPhotoAdapter.deleteById(bodyPhotoId)
    }

    companion object {
        private const val BODY_PHOTO_PATH = "body_photo"
    }

}