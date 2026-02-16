package app.thunder.domain.member

import app.thunder.domain.member.command.CreateMemberCommand

interface MemberPort {
    fun deleteById(memberId: Long)
    fun getAll(): List<Member>
    fun getAllByReviewRequestNotifyTrue(): List<Member>
    fun getById(memberId: Long): Member?
    fun getAllById(memberIds: Collection<Long>): List<Member>
    fun getByNickname(nickname: String): Member?
    fun getByMobileNumber(mobileNumber: String): Member?
    fun create(command: CreateMemberCommand): Member
    fun update(member: Member)
}