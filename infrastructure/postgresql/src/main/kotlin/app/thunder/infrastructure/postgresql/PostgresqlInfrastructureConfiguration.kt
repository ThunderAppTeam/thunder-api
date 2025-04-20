package app.thunder.infrastructure.postgresql

import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@Configuration
@ComponentScan(basePackages = ["app.thunder.infrastructure.postgresql"])
@EntityScan(basePackages = ["app.thunder.infrastructure.postgresql"])
@EnableJpaRepositories(basePackages = ["app.thunder.infrastructure.postgresql"])
class PostgresqlInfrastructureConfiguration
