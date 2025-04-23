package app.thunder.infrastructure.postgresql.member

import app.thunder.core.member.domain.MemberSetting
import app.thunder.core.member.domain.MemberSettingOptions
import app.thunder.infrastructure.postgresql.PostgresqlJpaTest
import jakarta.persistence.EntityManager
import java.time.LocalDateTime
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@PostgresqlJpaTest
class MemberSettingRepositoryImplTest {

    @Autowired
    private lateinit var repository: MemberSettingRepositoryImpl

    @Autowired
    private lateinit var entityManager: EntityManager

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

    @Test
    @DisplayName("설정을 수정하면 알림 동의와 마케팅 동의, 갱신 시각이 저장되고 다른 회원의 설정은 그대로이다")
    fun update() {
        // given
        val options = MemberSettingOptions(reviewCompleteNotify = true, reviewRequestNotify = true, marketingAgreement = false)
        val created = repository.create(MEMBER_ID, options)
        repository.create(OTHER_MEMBER_ID, options)
        val updatedAt = LocalDateTime.of(2026, 1, 2, 3, 4, 5)
        val changed = MemberSetting(
            memberId = MEMBER_ID,
            reviewCompleteNotify = false,
            reviewRequestNotify = false,
            marketingAgreement = true,
            createdAt = created.createdAt,
            updatedAt = updatedAt,
        )

        // when
        repository.update(changed)
        entityManager.flush()
        entityManager.clear()

        // then
        val reloaded = repository.getByMemberId(MEMBER_ID)!!
        assertThat(reloaded.reviewCompleteNotify).isFalse()
        assertThat(reloaded.reviewRequestNotify).isFalse()
        assertThat(reloaded.marketingAgreement).isTrue()
        assertThat(reloaded.updatedAt).isEqualTo(updatedAt)
        val other = repository.getByMemberId(OTHER_MEMBER_ID)!!
        assertThat(other.reviewCompleteNotify).isTrue()
        assertThat(other.reviewRequestNotify).isTrue()
        assertThat(other.marketingAgreement).isFalse()
        assertThat(other.updatedAt).isNull()
    }

    @Test
    @DisplayName("저장되지 않은 회원의 설정을 수정해도 새 설정이 만들어지지 않는다")
    fun updateMissingMemberSetting() {
        // given
        val missing = MemberSetting(
            memberId = MEMBER_ID,
            reviewCompleteNotify = true,
            reviewRequestNotify = true,
            marketingAgreement = true,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
        )

        // when
        repository.update(missing)
        entityManager.flush()
        entityManager.clear()

        // then
        assertThat(repository.getByMemberId(MEMBER_ID)).isNull()
    }

    companion object {
        private const val MEMBER_ID = 7_301L
        private const val OTHER_MEMBER_ID = 7_302L
    }

}
