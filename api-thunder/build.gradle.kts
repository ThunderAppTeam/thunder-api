plugins {
    id("thunder.runtime")
}

dependencies {
    implementation(project(":core"))
    implementation(project(":libraries:errors"))
    implementation(project(":infrastructure:postgresql"))
    implementation(project(":infrastructure:aws"))
    implementation(project(":infrastructure:firebase"))
    implementation(project(":infrastructure:client"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation(libs.kotlin.jdsl.jpql.dsl)
    implementation(libs.kotlin.jdsl.jpql.render)
    implementation(libs.kotlin.jdsl.spring.data.jpa.support)

    implementation(libs.jjwt.api)
    runtimeOnly(libs.jjwt.impl)
    runtimeOnly(libs.jjwt.jackson)

    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly(libs.flyway.database.postgresql)

    implementation(libs.thumbnailator)
    implementation(libs.datafaker)

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.springframework.restdocs:spring-restdocs-mockmvc")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation(libs.firebase.admin)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
