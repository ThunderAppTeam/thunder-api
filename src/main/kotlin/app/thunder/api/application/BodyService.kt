package app.thunder.api.application

import app.thunder.api.adapter.storage.StorageAdapter
import app.thunder.api.controller.response.PostReviewRefreshResponse
import app.thunder.api.domain.body.BodyPhoto
import app.thunder.api.domain.body.BodyPhotoAdapter
import app.thunder.api.domain.body.BodyReviewAdapter
import app.thunder.api.domain.body.ReviewRotationAdapter
import app.thunder.api.domain.member.MemberAdapter
import app.thunder.api.exception.BodyErrors
import app.thunder.api.exception.BodyErrors.ALREADY_REVIEWED
import app.thunder.api.exception.BodyErrors.UNSUPPORTED_IMAGE_FORMAT
import app.thunder.api.exception.MemberErrors.NOT_FOUND_MEMBER
import app.thunder.api.exception.ThunderException
import java.util.UUID
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile

@Service
class BodyService(
    private val memberAdapter: MemberAdapter,
    private val bodyPhotoAdapter: BodyPhotoAdapter,
    private val bodyReviewAdapter: BodyReviewAdapter,
    private val reviewRotationAdapter: ReviewRotationAdapter,
    private val storageAdapter: StorageAdapter,
) {

    companion object {
        private const val BODY_PHOTO_PATH = "body_photo"
        private const val REVIEW_COMPLETE_COUNT = 20L
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
    fun refreshReview(memberId: Long, refreshCount: Int): List<PostReviewRefreshResponse> {
        val bodyPhotoIdSet = linkedSetOf<Long>()
        val fetchSize = 5
        var reviewRotationId = 0L
        while (bodyPhotoIdSet.size < refreshCount) {
            val reviewRotations = reviewRotationAdapter
                .getAllByIdGteAndMemberIdNot(reviewRotationId, memberId, fetchSize)
            if (reviewRotations.isEmpty()) {
                break
            }
            reviewRotations.asSequence()
                .filter { !it.reviewedMemberIds.contains(memberId) }
                .take(refreshCount - bodyPhotoIdSet.size)
                .forEach { bodyPhotoIdSet.add(it.bodyPhotoId) }

            reviewRotationId += fetchSize
        }
        reviewRotationAdapter.refresh(bodyPhotoIdSet)

        val bodyPhotoMap = bodyPhotoAdapter.getAllById(bodyPhotoIdSet)
            .associateBy { it.bodyPhotoId }
        val memberIdSet = bodyPhotoMap.values.map { it.memberId }.toSet()
        val memberMap = memberAdapter.getAllById(memberIdSet)
            .associateBy { it.memberId }
        return bodyPhotoIdSet.map { bodyPhotoId ->
            val bodyPhoto = bodyPhotoMap[bodyPhotoId] ?: throw ThunderException(BodyErrors.NOT_FOUND_BODY_PHOTO)
            val member = memberMap[bodyPhoto.memberId] ?: throw ThunderException(NOT_FOUND_MEMBER)
            PostReviewRefreshResponse(bodyPhoto.bodyPhotoId,
                                      bodyPhoto.imageUrl,
                                      member.memberId,
                                      member.nickname,
                                      member.age)
        }
    }

    @Transactional
    fun review(bodyPhotoId: Long, memberId: Long, score: Int) {
        val bodyPhoto = bodyPhotoAdapter.getById(bodyPhotoId)
        if (bodyReviewAdapter.existsByBodyPhotoIdAndMemberId(bodyPhotoId, memberId)) {
            throw ThunderException(ALREADY_REVIEWED)
        }

        val reviewCount = bodyReviewAdapter.getCountByBodyPhotoId(bodyPhotoId)
        if (reviewCount >= REVIEW_COMPLETE_COUNT - 1) {
            bodyPhoto.completeReview()
            bodyPhotoAdapter.update(bodyPhoto)
        }

        val member = memberAdapter.getById(memberId)
        bodyReviewAdapter.create(bodyPhotoId, member.memberId, score)

        val reviewRotation = reviewRotationAdapter.getByBodyPhotoId(bodyPhotoId)
        reviewRotation.addReviewedMember(memberId)
        reviewRotationAdapter.update(reviewRotation)
    }

}