package app.thunder.infrastructure.postgresql

import com.linecorp.kotlinjdsl.support.spring.data.jpa.autoconfigure.KotlinJdslAutoConfiguration
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.context.annotation.Import

@SpringBootConfiguration
@ImportAutoConfiguration(KotlinJdslAutoConfiguration::class)
@Import(PostgresqlInfrastructureConfiguration::class)
class PostgresqlTestApplication
