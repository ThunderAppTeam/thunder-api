package app.thunder.api.application

import app.thunder.api.controller.response.GetBodyPhotoResponse
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
    fun get(bodyPhotoId: Long, memberId: Long): GetBodyPhotoResponse {
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

        return GetBodyPhotoResponse(
            bodyPhotoId = bodyPhoto.bodyPhotoId,
            imageUrl = bodyPhoto.imageUrl,
            isReviewCompleted = bodyPhoto.isReviewCompleted,
            progressRate = reviewCount * 5.0,
            gender = member.gender,
            totalScore = round(bodyPhoto.reviewScore * 10) / 10,
            genderTopPercent = round(topPercent),
            genderTopRate = round(topPercent),
            createdAt = bodyPhoto.createdAt.toKoreaZonedDateTime(),
        )
    }

}