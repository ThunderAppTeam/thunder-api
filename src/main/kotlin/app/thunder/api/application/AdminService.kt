package app.thunder.api.application

import app.thunder.api.domain.flag.FlagHistoryRepository
import app.thunder.api.domain.member.repository.MemberBlockRelationRepository
import app.thunder.api.domain.member.repository.MemberRepository
import app.thunder.api.domain.photo.BodyPhotoRepository
import app.thunder.api.domain.photo.findAllByMemberId
import app.thunder.api.domain.review.repository.BodyReviewRepository
import app.thunder.api.event.RefreshReviewableEvent
import app.thunder.api.exception.MemberErrors.NOT_FOUND_MEMBER
import app.thunder.api.exception.ThunderException
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class AdminService(
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val memberRepository: MemberRepository,
    private val bodyReviewRepository: BodyReviewRepository,
    private val bodyPhotoRepository: BodyPhotoRepository,
    private val flagHistoryRepository: FlagHistoryRepository,
    private val memberBlockRelationRepository: MemberBlockRelationRepository,
) {

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun reset(target: String, mobileNumber: String) {
        val memberId = this.resetTarget(target, mobileNumber)
        applicationEventPublisher.publishEvent(RefreshReviewableEvent(memberId))
    }

    @Transactional
    fun resetTarget(target: String, mobileNumber: String): Long {
        val member = memberRepository.findByMobileNumber(mobileNumber)
            ?: throw ThunderException(NOT_FOUND_MEMBER)
        val memberId = member.memberId

        when (target) {
            "REVIEW" -> {
                bodyReviewRepository.deleteAll()
                bodyPhotoRepository.findAllByMemberId(memberId)
                    .forEach { bodyPhotoEntity ->
                        bodyPhotoEntity.update(0, 0.0, bodyPhotoEntity.updatedAt)
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