package app.thunder.api

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext

class ThunderApiApplicationTest : ThunderApiWebTest() {

    @Autowired
    lateinit var applicationContext: ApplicationContext

    @Test
    @DisplayName("빈 PostgreSQL에 migration을 적용한 뒤 모든 Bean이 연결된 상태로 애플리케이션이 기동한다")
    fun contextLoads() {
        // given
        val applicationClass = ThunderApiApplication::class.java

        // when
        val application = applicationContext.getBean(applicationClass)

        // then
        assertThat(application).isNotNull
    }

}
