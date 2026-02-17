package app.thunder.api

import java.time.ZoneOffset.UTC
import java.util.TimeZone
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling

@EnableScheduling
@EnableAsync
@ConfigurationPropertiesScan(basePackages = ["app.thunder"])
@EntityScan(basePackages = ["app.thunder.infrastructure.db"])
@EnableJpaRepositories(basePackages = ["app.thunder.infrastructure.db"])
@SpringBootApplication(scanBasePackages = ["app.thunder.api", "app.thunder.infrastructure"])
class ThunderApiApplication

fun main(args: Array<String>) {
    TimeZone.setDefault(TimeZone.getTimeZone(UTC))
    runApplication<ThunderApiApplication>(*args)
}
