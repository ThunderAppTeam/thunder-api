package app.thunder.infrastructure.firebase

import app.thunder.infrastructure.firebase.notification.FirebaseProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(FirebaseProperties::class)
@ComponentScan(basePackages = ["app.thunder.infrastructure.firebase"])
class FirebaseInfrastructureConfiguration
