package app.thunder.infrastructure.postgresql.member

import app.thunder.core.member.domain.MemberSettingOptions
import app.thunder.infrastructure.postgresql.PostgresqlJpaTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@PostgresqlJpaTest
class MemberSettingRepositoryImplTest {

    @Autowired
    private lateinit var repository: MemberSettingRepositoryImpl

    @Test
    @DisplayName("회원의 설정을 지우면 그 회원의 설정만 사라진다")
    fun deleteByMemberId() {
        // given
        val options = MemberSettingOptions(reviewCompleteNotify = true, reviewRequestNotify = true, marketingAgreement = false)
        repository.create(MEMBER_ID, options)
        repository.create(OTHER_MEMBER_ID, options)

        // when
        repository.deleteByMemberId(MEMBER_ID)

        // then
        assertThat(repository.getByMemberId(MEMBER_ID)).isNull()
        assertThat(repository.getByMemberId(OTHER_MEMBER_ID)).isNotNull()
    }

    companion object {
        private const val MEMBER_ID = 7_301L
        private const val OTHER_MEMBER_ID = 7_302L
    }

}
