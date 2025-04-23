package app.thunder.api.member

import app.thunder.api.ThunderApiWebTest
import app.thunder.api.application.MemberService
import app.thunder.libraries.errors.MemberErrors.NOT_FOUND_MEMBER
import app.thunder.libraries.errors.ThunderException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class MemberLookupTest : ThunderApiWebTest() {

    @Autowired
    private lateinit var memberService: MemberService

    @Test
    @DisplayName("존재하지 않는 회원을 조회하면 NOT_FOUND_MEMBER 예외를 던진다")
    fun getByIdNotFound() {
        // given
        val unknownMemberId = 900_931L

        // when
        val thrown = assertThatThrownBy { memberService.getById(unknownMemberId) }

        // then
        thrown.isInstanceOfSatisfying(ThunderException::class.java) {
            assertThat(it.errorCode).isEqualTo(NOT_FOUND_MEMBER)
        }
    }

}
