package app.thunder.api.application

import app.thunder.api.domain.body.ReviewRotationQueueRepository
import app.thunder.api.domain.flag.FlagHistoryRepository
import app.thunder.api.domain.member.MemberBlockRelationRepository
import app.thunder.api.domain.member.MemberRepository
import app.thunder.api.domain.photo.BodyPhotoRepository
import app.thunder.api.domain.photo.findAllByMemberId
import app.thunder.api.domain.review.BodyReviewRepository
import app.thunder.api.exception.MemberErrors.NOT_FOUND_MEMBER
import app.thunder.api.exception.ThunderException
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class AdminService(
    private val memberRepository: MemberRepository,
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val bodyReviewRepository: BodyReviewRepository,
    private val bodyPhotoRepository: BodyPhotoRepository,
    private val flagHistoryRepository: FlagHistoryRepository,
    private val memberBlockRelationRepository: MemberBlockRelationRepository,
    private val reviewRotationQueueRepository: ReviewRotationQueueRepository,
) {

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun reset(target: String, mobileNumber: String) {
        val memberId = this.resetTarget(target, mobileNumber)
        applicationEventPublisher.publishEvent(SupplyReviewableEvent(memberId))
    }

    @Transactional
    fun resetTarget(target: String, mobileNumber: String): Long {
        val member = memberRepository.findByMobileNumber(mobileNumber)
            .orElseThrow { ThunderException(NOT_FOUND_MEMBER) }
        val memberId = member.memberId

        when (target) {
            "REVIEW" -> {
                bodyReviewRepository.deleteAll()
                bodyPhotoRepository.findAllByMemberId(memberId)
                    .forEach { bodyPhotoEntity ->
                        bodyPhotoEntity.update(0, 0.0, bodyPhotoEntity.updatedAt)
                    }

                reviewRotationQueueRepository.findAll()
                    .forEach { reviewRotationEntity ->
                        reviewRotationEntity.removeMemberId(memberId)
                    }
            }

            "FLAG" -> {
                val flagHistoryIds = flagHistoryRepository.findAllByMemberId(memberId).map { it.flagHistoryId }
                flagHistoryRepository.deleteAllById(flagHistoryIds)
            }

            "BLOCK" -> {
                val ids = memberBlockRelationRepository.findAllByMemberId(memberId).map { it.memberBlockRelationId }
                memberBlockRelationRepository.deleteAllById(ids)
            }
        }
        return memberId
    }

}