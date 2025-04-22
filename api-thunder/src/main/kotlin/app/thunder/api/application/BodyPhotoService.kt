package app.thunder.api.application

import app.thunder.api.controller.response.GetBodyPhotoResponse
import app.thunder.api.controller.response.GetBodyPhotoResultResponse
import app.thunder.api.event.RefreshReviewableEvent
import app.thunder.api.event.ReviewUploadEvent
import app.thunder.api.func.toKoreaZonedDateTime
import app.thunder.core.flag.repository.FlagHistoryRepository
import app.thunder.core.member.repository.MemberRepository
import app.thunder.core.photo.client.BodyDetectionClient
import app.thunder.core.photo.domain.BodyPhoto
import app.thunder.core.photo.domain.BodyPhoto.Companion.RANKING_PERIOD_DAYS
import app.thunder.core.photo.repository.BodyPhotoRepository
import app.thunder.core.photo.client.BodyPhotoStorageClient
import app.thunder.core.review.repository.BodyReviewRepository
import app.thunder.core.review.repository.ReviewableBodyPhotoRepository
import app.thunder.libraries.errors.BodyErrors.BODY_NOT_DETECTED_IN_PHOTO
import app.thunder.libraries.errors.BodyErrors.IMAGE_TOO_LARGE
import app.thunder.libraries.errors.BodyErrors.NOT_FOUND_BODY_PHOTO
import app.thunder.libraries.errors.BodyErrors.UNSUPPORTED_IMAGE_FORMAT
import app.thunder.libraries.errors.BodyErrors.UPLOADER_OR_ADMIN_ONLY_ACCESS
import app.thunder.libraries.errors.MemberErrors.NOT_FOUND_MEMBER
import app.thunder.libraries.errors.ThunderException
import java.io.ByteArrayOutputStream
import java.time.LocalDateTime
import java.util.UUID
import kotlin.math.round
import net.coobird.thumbnailator.Thumbnails
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile

@Service
class BodyPhotoService(
    private val memberRepository: MemberRepository,
    private val bodyPhotoRepository: BodyPhotoRepository,
    private val bodyPhotoStorageClient: BodyPhotoStorageClient,
    private val reviewableBodyPhotoRepository: ReviewableBodyPhotoRepository,
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val bodyDetectionClient: BodyDetectionClient,
    private val bodyReviewRepository: BodyReviewRepository,
    private val flagHistoryRepository: FlagHistoryRepository,
    private val bodyPhotoStorageCleaner: BodyPhotoStorageCleaner,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional(readOnly = true)
    fun getAllByMemberId(memberId: Long): List<GetBodyPhotoResponse> {
        return bodyPhotoRepository.getAllByMemberId(memberId).map {
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

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    fun getByBodyPhotoId(bodyPhotoId: Long, memberId: Long, isAdmin: Boolean): GetBodyPhotoResultResponse {
        val bodyPhoto = bodyPhotoRepository.getById(bodyPhotoId)
            ?: throw ThunderException(NOT_FOUND_BODY_PHOTO)
        if (bodyPhoto.isNotUploader(memberId) && !isAdmin) {
            throw ThunderException(UPLOADER_OR_ADMIN_ONLY_ACCESS)
        }
        val owner = memberRepository.getById(bodyPhoto.memberId)
            ?: throw ThunderException(NOT_FOUND_MEMBER)

        val since = LocalDateTime.now().minusDays(RANKING_PERIOD_DAYS)
        val poolCount = bodyPhotoRepository.countReviewCompletedByGender(owner.gender, since)
        val totalCount = if (bodyPhoto.isInRankingPool(since)) poolCount else poolCount + 1
        val higherCount = bodyPhotoRepository.countReviewCompletedByGenderWithHigherAverageScore(
            owner.gender, bodyPhoto.averageScore(), since,
        )
        val ranking = higherCount + 1
        val genderTopPercent = ranking.toDouble() / totalCount * 100

        return GetBodyPhotoResultResponse(
            bodyPhotoId = bodyPhoto.bodyPhotoId,
            imageUrl = bodyPhoto.imageUrl,
            isReviewCompleted = bodyPhoto.isReviewCompleted(),
            reviewCount = bodyPhoto.reviewCount,
            progressRate = bodyPhoto.progressRate(),
            gender = owner.gender,
            reviewScore = bodyPhoto.getResultReviewScore(),
            genderTopRate = round(genderTopPercent),
            createdAt = bodyPhoto.createdAt.toKoreaZonedDateTime(),
        )
    }

    @Transactional
    fun upload(imageFile: MultipartFile, memberId: Long): BodyPhoto {
        val extension = IMAGE_EXTENSIONS[imageFile.contentType]
            ?: throw ThunderException(UNSUPPORTED_IMAGE_FORMAT)

        val imageBytes = prepareImageBytes(imageFile.bytes)
        val bodyDetectionResult = bodyDetectionClient.detectBody(imageBytes)
        if (!bodyDetectionResult.isDetectedBody) {
            log.error("Body Rekognition Failed [memberId: $memberId]\n{}", bodyDetectionResult.details)
            throw ThunderException(BODY_NOT_DETECTED_IN_PHOTO)
        }

        val filePath = "$memberId/$BODY_PHOTO_PATH/${UUID.randomUUID()}.$extension"
        val imageUrl = bodyPhotoStorageClient.upload(imageBytes, imageFile.contentType, filePath)
        bodyPhotoStorageCleaner.deleteOnRollback(imageUrl)

        reviewableBodyPhotoRepository.lockQueue(memberId)
        memberRepository.getById(memberId)
            ?: throw ThunderException(NOT_FOUND_MEMBER)
        val bodyPhoto = bodyPhotoRepository.create(memberId, imageUrl)
        applicationEventPublisher.publishEvent(ReviewUploadEvent(bodyPhoto.bodyPhotoId))
        return bodyPhoto
    }

    private fun prepareImageBytes(imageBytes: ByteArray): ByteArray {
        if (imageBytes.size < IMAGE_SIZE_LIMIT_BYTES) {
            return imageBytes
        }
        val optimized = optimizeImageBytes(imageBytes)
        if (optimized.size >= IMAGE_SIZE_LIMIT_BYTES) {
            throw ThunderException(IMAGE_TOO_LARGE)
        }
        return optimized
    }

    private fun optimizeImageBytes(imageBytes: ByteArray): ByteArray {
        val outputStream = ByteArrayOutputStream()
        Thumbnails.of(imageBytes.inputStream())
            .size(1080, 1920)
            .outputQuality(0.9)
            .toOutputStream(outputStream)
        return outputStream.toByteArray()
    }

    @Transactional
    fun deleteByBodyPhotoId(bodyPhotoId: Long, memberId: Long) {
        val bodyPhoto = bodyPhotoRepository.getById(bodyPhotoId)
            ?: throw ThunderException(NOT_FOUND_BODY_PHOTO)
        if (bodyPhoto.isNotUploader(memberId)) {
            throw ThunderException(UPLOADER_OR_ADMIN_ONLY_ACCESS)
        }
        bodyPhotoRepository.deleteById(bodyPhotoId)
        bodyReviewRepository.deleteAllByBodyPhotoId(bodyPhotoId)
        flagHistoryRepository.deleteAllByBodyPhotoId(bodyPhotoId)

        val reviewableList = reviewableBodyPhotoRepository.getAllByBodyPhotoId(bodyPhotoId)
        reviewableList.forEach {
            applicationEventPublisher.publishEvent(RefreshReviewableEvent(it.memberId))
        }
        reviewableBodyPhotoRepository.deleteAllByBodyPhotoId(bodyPhotoId)
        bodyPhotoStorageCleaner.deleteAfterCommit(listOf(bodyPhoto.imageUrl))
    }

    companion object {
        private const val BODY_PHOTO_PATH = "body_photo"
        private const val IMAGE_SIZE_LIMIT_BYTES = 5 * 1024 * 1024
        private val IMAGE_EXTENSIONS = mapOf(
            MediaType.IMAGE_JPEG_VALUE to "jpg",
            MediaType.IMAGE_PNG_VALUE to "png",
        )
    }

}
