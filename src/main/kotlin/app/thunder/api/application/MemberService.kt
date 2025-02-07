package app.thunder.api.application

import app.thunder.api.application.SupplyReviewableEventHandler.Companion.REVIEWABLE_QUEUE_MINIMUM_SIZE
import app.thunder.api.domain.review.adapter.ReviewableBodyPhotoAdapter
import app.thunder.api.domain.member.Member
import app.thunder.api.domain.member.adapter.MemberAdapter
import app.thunder.api.domain.member.adapter.MemberBlockRelationAdapter
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate

@Service
class MemberService(
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val memberAdapter: MemberAdapter,
    private val memberBlockRelationAdapter: MemberBlockRelationAdapter,
    private val reviewableBodyPhotoAdapter: ReviewableBodyPhotoAdapter,
    private val transactionTemplate: TransactionTemplate,
) {

    @Transactional(readOnly = true)
    fun getById(memberId: Long): Member {
        return memberAdapter.getById(memberId)
    }

    fun block(requestMemberId: Long, blockedMemberId: Long) {
        transactionTemplate.execute { status ->
            memberAdapter.getById(blockedMemberId)
            memberBlockRelationAdapter.create(memberId = blockedMemberId,
                                              blockedMemberId = requestMemberId,
                                              createdBy = requestMemberId)
            memberBlockRelationAdapter.create(memberId = requestMemberId,
                                              blockedMemberId = blockedMemberId,
                                              createdBy = requestMemberId)
            status.flush()
            true
        }

        val deleteAllBlockedMemberBodyPhotoInQueue = { blocker: Long, blocked: Long ->
            reviewableBodyPhotoAdapter.getAllByMemberId(blocker).asSequence()
                .filter { it.bodyPhotoMemberId == blocked }
                .forEach { reviewableBodyPhotoAdapter.deleteByMemberIdAndBodyPhotoId(blocker, it.bodyPhotoId) }
            val blockedMemberQueueSize = reviewableBodyPhotoAdapter.getAllByMemberId(blocker).size
            if (blockedMemberQueueSize <= REVIEWABLE_QUEUE_MINIMUM_SIZE) {
                applicationEventPublisher.publishEvent(SupplyReviewableEvent(blocker))
            }
        }

        transactionTemplate.execute { status ->
            deleteAllBlockedMemberBodyPhotoInQueue(blockedMemberId, requestMemberId)
            deleteAllBlockedMemberBodyPhotoInQueue(requestMemberId, blockedMemberId)
            status.flush()
            true
        }
    }

}