package app.thunder.core.member.repository

import app.thunder.core.member.command.CreateMemberCommand
import app.thunder.core.member.domain.Member
import java.time.LocalDateTime

interface MemberRepository {
    fun deleteById(memberId: Long)
    fun getAll(): List<Member>
    fun getAllByReviewRequestNotifyTrue(): List<Member>
    fun getById(memberId: Long): Member?
    fun getAllById(memberIds: Collection<Long>): List<Member>
    fun getByNickname(nickname: String): Member?
    fun getByMobileNumber(mobileNumber: String): Member?
    fun create(command: CreateMemberCommand): Member
    fun revokeTokensIssuedBefore(memberId: Long, revokedAt: LocalDateTime)
}