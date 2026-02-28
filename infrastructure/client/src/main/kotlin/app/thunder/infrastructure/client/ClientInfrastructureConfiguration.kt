package app.thunder.infrastructure.client

import app.thunder.infrastructure.client.sms.AligoProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(AligoProperties::class)
@ComponentScan(basePackages = ["app.thunder.infrastructure.client"])
class ClientInfrastructureConfiguration
