package app.thunder.api.domain.review.adapter

import app.thunder.api.domain.flag.FlagHistoryRepository
import app.thunder.api.domain.member.repository.MemberBlockRelationRepository
import app.thunder.api.domain.photo.BodyPhotoRepository
import app.thunder.api.domain.photo.findAllNotReviewCompleted
import app.thunder.api.domain.review.ReviewableBodyPhoto
import app.thunder.api.domain.review.entity.ReviewableBodyPhotoEntity
import app.thunder.api.domain.review.entity.ReviewableBodyPhotoId
import app.thunder.api.domain.review.repository.BodyReviewRepository
import app.thunder.api.domain.review.repository.ReviewableBodyPhotoRepository
import app.thunder.api.domain.review.repository.findAllByMemberId
import app.thunder.api.domain.review.repository.findFirstAllByMemberIds
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class ReviewableBodyPhotoAdapter(
    private val reviewableBodyPhotoRepository: ReviewableBodyPhotoRepository,
    private val bodyPhotoRepository: BodyPhotoRepository,
    private val bodyReviewRepository: BodyReviewRepository,
    private val flagHistoryRepository: FlagHistoryRepository,
    private val memberBlockRelationRepository: MemberBlockRelationRepository,
) {

    @Transactional(readOnly = true)
    fun getAllByMemberId(memberId: Long, limit: Int? = null): List<ReviewableBodyPhoto> {
        return reviewableBodyPhotoRepository.findAllByMemberId(memberId, limit)
            .map(ReviewableBodyPhoto::from)
    }

    @Transactional(readOnly = true)
    fun getAllByBodyPhotoId(bodyPhotoId: Long): List<ReviewableBodyPhoto> {
        return reviewableBodyPhotoRepository.findAllByBodyPhotoId(bodyPhotoId)
            .map(ReviewableBodyPhoto::from)
    }

    @Transactional(readOnly = true)
    fun getFirstByMemberIds(memberIds: Collection<Long>): List<ReviewableBodyPhoto> {
        return reviewableBodyPhotoRepository.findFirstAllByMemberIds(memberIds)
            .map(ReviewableBodyPhoto::from)
    }

    @Transactional
    fun create(memberId: Long, bodyPhotoId: Long, bodyPhotoMemberId: Long) {
        val entity = ReviewableBodyPhotoEntity.create(memberId, bodyPhotoId, bodyPhotoMemberId)
        reviewableBodyPhotoRepository.save(entity)
    }

    @Transactional
    fun deleteByMemberIdAndBodyPhotoId(memberId: Long, bodyPhotoId: Long) {
        reviewableBodyPhotoRepository.deleteByMemberIdAndBodyPhotoId(memberId, bodyPhotoId)
    }

    @Transactional
    fun deleteAllByMemberId(memberId: Long) {
        val ids = hashSetOf<ReviewableBodyPhotoId>()
        reviewableBodyPhotoRepository.findAllByMemberId(memberId)
            .forEach { ids.add(ReviewableBodyPhotoId(it.memberId, it.createdAt)) }
        reviewableBodyPhotoRepository.findAllByBodyPhotoMemberId(memberId)
            .forEach { ids.add(ReviewableBodyPhotoId(it.memberId, it.createdAt)) }
        reviewableBodyPhotoRepository.deleteAllByIdInBatch(ids)
    }

    @Transactional
    fun deleteAllByBodyPhotoId(bodyPhotoId: Long) {
        val ids = reviewableBodyPhotoRepository.findAllByBodyPhotoId(bodyPhotoId)
            .map { ReviewableBodyPhotoId(it.memberId, it.createdAt) }
        reviewableBodyPhotoRepository.deleteAllByIdInBatch(ids)
    }

    @Transactional
    fun deleteAllByMemberIdAndPhotoMemberId(memberId: Long, bodyPhotoMemberId: Long) {
        val reviewableBodyPhotoIds = reviewableBodyPhotoRepository.findAllByMemberId(memberId)
            .asSequence()
            .filter { it.bodyPhotoMemberId == bodyPhotoMemberId }
            .map { ReviewableBodyPhotoId(it.memberId, it.createdAt) }
            .toList()
        reviewableBodyPhotoRepository.deleteAllByIdInBatch(reviewableBodyPhotoIds)
    }

    @Transactional
    fun refresh(reviewMemberId: Long) {
        val suppliedBodyPhotoIdSet = reviewableBodyPhotoRepository.findAllByMemberId(reviewMemberId)
            .map { it.bodyPhotoId }.toSet()
        if (suppliedBodyPhotoIdSet.size > REVIEWABLE_QUEUE_MINIMUM_SIZE) {
            return
        }
        val supplySize = REVIEWABLE_QUEUE_MAXIMUM_SIZE - suppliedBodyPhotoIdSet.size

        val flagCountMap = hashMapOf<Long, Int>()
        val flaggedBodyPhotoIdSet = hashSetOf<Long>()
        flagHistoryRepository.findAll().forEach { flagHistory ->
            flagCountMap.merge(flagHistory.bodyPhotoId, 1, Int::plus)
            if (flagHistory.memberId == reviewMemberId) {
                flaggedBodyPhotoIdSet.add(flagHistory.bodyPhotoId)
            }
        }

        val reviewedBodyPhotoIdSet = bodyReviewRepository.findAllByMemberId(reviewMemberId)
            .map { it.bodyPhotoId }.toSet()
        val blockedMemberIdSet = memberBlockRelationRepository.findAllByMemberId(reviewMemberId)
            .map { it.blockedMemberId }.toSet()

        val filteredBodyPhotoList = bodyPhotoRepository.findAllNotReviewCompleted()
            .asSequence()
            .filter { it.memberId != reviewMemberId }
            .filter { !suppliedBodyPhotoIdSet.contains(it.bodyPhotoId) }
            .filter { !reviewedBodyPhotoIdSet.contains(it.bodyPhotoId) }
            .filter { !flaggedBodyPhotoIdSet.contains(it.bodyPhotoId) }
            .filter { (flagCountMap[it.bodyPhotoId] ?: 0) < 3 }
            .filter { !blockedMemberIdSet.contains(it.memberId) }
            .shuffled()
            .sortedBy { it.reviewCount }
            .take(supplySize)
            .toList()

        val reviewableBodyPhotoEntities = filteredBodyPhotoList.map {
            ReviewableBodyPhotoEntity.create(memberId = reviewMemberId,
                                             bodyPhotoId = it.bodyPhotoId,
                                             bodyPhotoMemberId = it.memberId)
        }
        // TODO: need to using jdbcTemplate for performance
        reviewableBodyPhotoRepository.saveAll(reviewableBodyPhotoEntities)
    }

    companion object {
        private const val REVIEWABLE_QUEUE_MAXIMUM_SIZE: Int = 30
        private const val REVIEWABLE_QUEUE_MINIMUM_SIZE: Int = 10
    }

}