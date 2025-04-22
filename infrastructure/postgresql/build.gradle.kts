plugins {
    id("thunder.spring")
    id("thunder.library")
}

dependencies {
    implementation(project(":core"))

    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.security:spring-security-crypto")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation(libs.kotlin.jdsl.jpql.dsl)
    implementation(libs.kotlin.jdsl.jpql.render)
    implementation(libs.kotlin.jdsl.spring.data.jpa.support)

    implementation(libs.datafaker)

    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation(libs.instancio.junit)
    testRuntimeOnly("org.postgresql:postgresql")
    testRuntimeOnly(libs.flyway.database.postgresql)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
