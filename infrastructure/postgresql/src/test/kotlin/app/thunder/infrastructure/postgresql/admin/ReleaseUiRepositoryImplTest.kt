package app.thunder.infrastructure.postgresql.admin

import app.thunder.infrastructure.postgresql.PostgresqlJpaTest
import app.thunder.infrastructure.postgresql.admin.entity.ReleaseUiMobileOs
import app.thunder.infrastructure.postgresql.admin.persistence.ReleaseUiJpaRepository
import java.time.LocalDateTime
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.instancio.Instancio
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate

@PostgresqlJpaTest
class ReleaseUiRepositoryImplTest {

    @Autowired
    private lateinit var repository: ReleaseUiRepositoryImpl

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var jpaRepository: ReleaseUiJpaRepository

    @Test
    @DisplayName("등록하지 않은 OS와 버전의 출시 여부는 false이다")
    fun getIsReleaseOfUnknownVersion() {
        // given
        val appVersion = appVersion()

        // when
        val isRelease = repository.getIsRelease("IOS", appVersion)

        // then
        assertThat(isRelease).isFalse()
    }

    @Test
    @DisplayName("출시 여부를 처음 저장하면 새 행이 만들어지고 저장한 값으로 조회된다")
    fun createOrUpdateCreatesRow() {
        // given
        val appVersion = appVersion()
        val updatedAt = LocalDateTime.of(2026, 1, 2, 3, 4, 5)

        // when
        repository.createOrUpdate("IOS", appVersion, true, updatedAt)

        // then
        assertThat(repository.getIsRelease("IOS", appVersion)).isTrue()
        val row = jdbcTemplate.queryForMap(
            "SELECT is_release, updated_at, created_at FROM release_ui WHERE mobile_os = 'IOS' AND app_version = ?",
            appVersion,
        )
        assertThat(row["is_release"]).isEqualTo(true)
        assertThat(row["updated_at"]).isEqualTo(java.sql.Timestamp.valueOf(updatedAt))
        assertThat(row["created_at"]).isNotNull()
    }

    @Test
    @DisplayName("이미 저장한 OS와 버전의 출시 여부를 다시 저장하면 새 행을 만들지 않고 값과 갱신 시각만 바뀐다")
    fun createOrUpdateUpdatesExistingRow() {
        // given
        val appVersion = appVersion()
        val firstUpdatedAt = LocalDateTime.of(2026, 1, 2, 3, 4, 5)
        val secondUpdatedAt = LocalDateTime.of(2026, 2, 3, 4, 5, 6)
        repository.createOrUpdate("ANDROID", appVersion, true, firstUpdatedAt)

        // when
        repository.createOrUpdate("ANDROID", appVersion, false, secondUpdatedAt)

        // then
        assertThat(repository.getIsRelease("ANDROID", appVersion)).isFalse()
        assertThat(rowCount("ANDROID", appVersion)).isEqualTo(1)
        val updatedAt = jdbcTemplate.queryForObject(
            "SELECT updated_at FROM release_ui WHERE mobile_os = 'ANDROID' AND app_version = ?",
            java.sql.Timestamp::class.java, appVersion,
        )
        assertThat(updatedAt).isEqualTo(java.sql.Timestamp.valueOf(secondUpdatedAt))
    }

    @Test
    @DisplayName("저장한 출시 여부를 엔티티로 조회하면 식별자와 갱신 시각이 담겨 있다")
    fun savedEntityCarriesIdAndUpdatedAt() {
        // given
        val appVersion = appVersion()
        val updatedAt = LocalDateTime.of(2026, 1, 2, 3, 4, 5)
        repository.createOrUpdate("IOS", appVersion, true, updatedAt)

        // when
        val entity = jpaRepository.findByMobileOsAndAppVersion(ReleaseUiMobileOs.IOS, appVersion)!!

        // then
        assertThat(entity.releaseUiId).isPositive()
        assertThat(entity.updatedAt).isEqualTo(updatedAt)
        assertThat(entity.isRelease).isTrue()
    }

    @Test
    @DisplayName("같은 버전이어도 OS가 다르면 출시 여부를 따로 저장한다")
    fun releaseFlagIsKeptPerMobileOs() {
        // given
        val appVersion = appVersion()
        val updatedAt = LocalDateTime.of(2026, 1, 2, 3, 4, 5)

        // when
        repository.createOrUpdate("IOS", appVersion, true, updatedAt)

        // then
        assertThat(repository.getIsRelease("IOS", appVersion)).isTrue()
        assertThat(repository.getIsRelease("ANDROID", appVersion)).isFalse()
    }

    @Test
    @DisplayName("지원하지 않는 OS로 조회하면 IllegalArgumentException이 발생한다")
    fun getIsReleaseWithUnsupportedOs() {
        // given
        val unsupportedOs = "WINDOWS"

        // when
        val thrown = assertThatThrownBy { repository.getIsRelease(unsupportedOs, appVersion()) }

        // then
        thrown.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    @DisplayName("지원하지 않는 OS로 저장하면 IllegalArgumentException이 발생하고 행이 만들어지지 않는다")
    fun createOrUpdateWithUnsupportedOs() {
        // given
        val unsupportedOs = "WINDOWS"
        val appVersion = appVersion()

        // when
        val thrown = assertThatThrownBy {
            repository.createOrUpdate(unsupportedOs, appVersion, true, LocalDateTime.now())
        }

        // then
        thrown.isInstanceOf(IllegalArgumentException::class.java)
        assertThat(rowCount(unsupportedOs, appVersion)).isEqualTo(0)
    }

    private fun appVersion(): String = Instancio.gen().string().digits().length(12).get()

    private fun rowCount(mobileOs: String, appVersion: String): Int = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM release_ui WHERE mobile_os = ? AND app_version = ?",
        Int::class.java, mobileOs, appVersion,
    )!!

}
