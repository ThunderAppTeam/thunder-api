package app.thunder.api.application

import app.thunder.api.domain.admin.MobileOs
import app.thunder.api.domain.admin.ReleaseUiEntity
import app.thunder.api.domain.admin.ReleaseUiRepository
import app.thunder.api.domain.flag.FlagHistoryRepository
import app.thunder.api.domain.member.repository.MemberBlockRelationRepository
import app.thunder.api.domain.review.repository.BodyReviewRepository
import app.thunder.api.event.RefreshReviewableEvent
import app.thunder.api.exception.MemberErrors.NOT_FOUND_MEMBER
import app.thunder.api.exception.ThunderException
import app.thunder.domain.photo.BodyPhotoAdapter
import app.thunder.storage.db.member.MemberRepository
import java.time.LocalDateTime
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AdminService(
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val memberRepository: MemberRepository,
    private val bodyReviewRepository: BodyReviewRepository,
    private val bodyPhotoAdapter: BodyPhotoAdapter,
    private val flagHistoryRepository: FlagHistoryRepository,
    private val memberBlockRelationRepository: MemberBlockRelationRepository,
    private val releaseUiRepository: ReleaseUiRepository,
) {

    @Transactional
    fun resetTarget(target: String, mobileNumber: String): Long {
        val member = memberRepository.findByMobileNumber(mobileNumber)
            ?: throw ThunderException(NOT_FOUND_MEMBER)
        val memberId = member.memberId

        when (target) {
            "REVIEW" -> {
                bodyReviewRepository.deleteAll()
                bodyPhotoAdapter.getAllByMemberId(memberId)
                    .forEach { bodyPhotoEntity ->
                        // TODO: change using Adapter
                        // bodyPhotoEntity.update(0, 0.0, bodyPhotoEntity.updatedAt)
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

        applicationEventPublisher.publishEvent(RefreshReviewableEvent(memberId))
        return memberId
    }

    @Transactional
    fun getReleaseUi(mobileOs: MobileOs, appVersion: String): Boolean {
        return releaseUiRepository.findByMobileOsAndAppVersion(mobileOs, appVersion)?.isRelease
            ?: false
    }

    @Transactional
    fun createOrUpdateReleaseUi(mobileOs: MobileOs, appVersion: String, isRelease: Boolean) {
        val releaseUiEntity = releaseUiRepository.findByMobileOsAndAppVersion(mobileOs, appVersion)
            ?: ReleaseUiEntity.create(mobileOs, appVersion)
        releaseUiEntity.update(isRelease, LocalDateTime.now())
        releaseUiRepository.save(releaseUiEntity)
    }

}