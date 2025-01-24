package app.thunder.api.application

import app.thunder.api.controller.response.GetBodyPhotoResponse
import app.thunder.api.controller.response.GetBodyPhotoResultResponse
import app.thunder.api.domain.body.BodyPhotoAdapter
import app.thunder.api.domain.body.BodyReviewAdapter
import app.thunder.api.domain.member.MemberAdapter
import app.thunder.api.func.toKoreaZonedDateTime
import kotlin.math.round
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BodyCheckService(
    private val bodyPhotoAdapter: BodyPhotoAdapter,
    private val memberAdapter: MemberAdapter,
    private val bodyReviewAdapter: BodyReviewAdapter,
) {

    @Transactional(readOnly = true)
    fun getAllByMemberId(memberId: Long): List<GetBodyPhotoResponse> {
        return bodyPhotoAdapter.getAllByMemberId(memberId).map {
            GetBodyPhotoResponse(
                bodyPhotoId = it.bodyPhotoId,
                imageUrl = it.imageUrl,
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

        return GetBodyPhotoResultResponse(
            bodyPhotoId = bodyPhoto.bodyPhotoId,
            imageUrl = bodyPhoto.imageUrl,
            isReviewCompleted = bodyPhoto.isReviewCompleted,
            reviewCount = reviewCount,
            progressRate = reviewCount / 20 * 100.0,
            gender = member.gender,
            reviewScore = round(bodyPhoto.reviewScore * 10) / 10,
            genderTopRate = round(topPercent),
            createdAt = bodyPhoto.createdAt.toKoreaZonedDateTime(),
        )
    }

}