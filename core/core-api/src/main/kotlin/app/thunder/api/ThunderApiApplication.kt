package app.thunder.api

import app.thunder.infrastructure.aws.AwsInfrastructureConfiguration
import app.thunder.infrastructure.client.ClientInfrastructureConfiguration
import app.thunder.infrastructure.db.DbCoreInfrastructureConfiguration
import app.thunder.infrastructure.firebase.FirebaseInfrastructureConfiguration
import java.time.ZoneOffset.UTC
import java.util.TimeZone
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Import
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling

@EnableScheduling
@EnableAsync
@ConfigurationPropertiesScan(basePackages = ["app.thunder.api"])
@Import(
    value = [
        AwsInfrastructureConfiguration::class,
        ClientInfrastructureConfiguration::class,
        FirebaseInfrastructureConfiguration::class,
        DbCoreInfrastructureConfiguration::class,
    ]
)
@SpringBootApplication(scanBasePackages = ["app.thunder.api"])
class ThunderApiApplication

fun main(args: Array<String>) {
    TimeZone.setDefault(TimeZone.getTimeZone(UTC))
    runApplication<ThunderApiApplication>(*args)
}
