import io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension

plugins {
    id("thunder.kotlin")
    id("org.jetbrains.kotlin.plugin.spring")
    id("org.jetbrains.kotlin.plugin.jpa")
    id("io.spring.dependency-management")
    id("org.springframework.boot")
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
val testcontainersVersion = libs.findVersion("testcontainers").get().requiredVersion
ext["testcontainers.version"] = testcontainersVersion

configure<DependencyManagementExtension> {
    imports {
        mavenBom("org.testcontainers:testcontainers-bom:$testcontainersVersion")
    }
}

dependencies {
    "implementation"("org.jetbrains.kotlin:kotlin-reflect")
    "implementation"("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    "implementation"("com.fasterxml.jackson.module:jackson-module-kotlin")
    "testImplementation"("org.springframework.boot:spring-boot-starter-test")
}
