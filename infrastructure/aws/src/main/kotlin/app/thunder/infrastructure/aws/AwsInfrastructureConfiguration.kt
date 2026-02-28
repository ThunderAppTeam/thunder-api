package app.thunder.infrastructure.aws

import app.thunder.infrastructure.aws.storage.AwsProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(AwsProperties::class)
@ComponentScan(basePackages = ["app.thunder.infrastructure.aws"])
class AwsInfrastructureConfiguration
