package app.thunder.infrastructure.db

import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@Configuration
@ComponentScan(basePackages = ["app.thunder.infrastructure.db"])
@EntityScan(basePackages = ["app.thunder.infrastructure.db"])
@EnableJpaRepositories(basePackages = ["app.thunder.infrastructure.db"])
class DbCoreInfrastructureConfiguration
