package app.thunder.api.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.context.properties.bind.Binder
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource

class AdminPropertiesTest {

    @Test
    @DisplayName("관리자 ID 설정이 비어 있으면 관리자가 없다")
    fun emptyMeansNoAdmins() {
        // given
        val source = MapConfigurationPropertySource(mapOf("admin.member-ids" to ""))

        // when
        val properties = Binder(source).bind("admin", AdminProperties::class.java).get()

        // then
        assertThat(properties.memberIds).isEmpty()
    }

    @Test
    @DisplayName("관리자 ID를 쉼표로 구분해 여러 개 지정할 수 있다")
    fun commaSeparatedIds() {
        // given
        val source = MapConfigurationPropertySource(mapOf("admin.member-ids" to "1, 22,333"))

        // when
        val properties = Binder(source).bind("admin", AdminProperties::class.java).get()

        // then
        assertThat(properties.memberIds).containsExactly(1L, 22L, 333L)
    }

}
