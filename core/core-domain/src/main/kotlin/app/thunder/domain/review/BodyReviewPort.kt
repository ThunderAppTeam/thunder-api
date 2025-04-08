package app.thunder.domain.review

interface BodyReviewPort {
    fun getAllByMemberId(memberId: Long): List<BodyReview>
    fun getMemberIdToLatestReviewMap(): Map<Long, BodyReview>
    fun existsByBodyPhotoIdAndMemberId(bodyPhotoId: Long, memberId: Long): Boolean
    fun create(bodyPhotoId: Long, memberId: Long, score: Int): BodyReview
}
