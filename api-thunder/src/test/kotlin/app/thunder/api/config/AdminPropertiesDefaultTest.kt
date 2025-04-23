package app.thunder.api.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class AdminPropertiesDefaultTest {

    @Test
    @DisplayName("관리자 ID를 지정하지 않고 만들면 관리자가 없다")
    fun defaultsToNoAdmins() {
        // given
        val properties = AdminProperties()

        // when
        val memberIds = properties.memberIds

        // then
        assertThat(memberIds).isEmpty()
    }

}
