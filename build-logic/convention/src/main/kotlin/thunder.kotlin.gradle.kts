import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    jacoco
    id("org.jetbrains.kotlin.jvm")
}

group = providers.gradleProperty("projectGroup").get()
version = providers.gradleProperty("applicationVersion").get()

repositories {
    mavenCentral()
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
val javaVersion = libs.findVersion("java").get().requiredVersion

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(javaVersion.toInt()))
    }
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xjsr305=strict",
            "-Xjvm-default=all",
        )
        jvmTarget.set(JvmTarget.fromTarget(javaVersion))
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
