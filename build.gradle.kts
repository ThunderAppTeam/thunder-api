import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    kotlin("plugin.spring") apply false
    kotlin("plugin.jpa") apply false
    id("org.springframework.boot") apply false
    id("io.spring.dependency-management") apply false
    id("org.asciidoctor.jvm.convert") apply false
}

group = "app.thunder"
version = "0.0.1"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

extra["snippetsDir"] = file("build/generated-snippets")

allprojects {
    group = "${property("projectGroup")}"
    version = "${property("applicationVersion")}"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply {
        plugin("org.jetbrains.kotlin.jvm")
        plugin("org.jetbrains.kotlin.kapt")
        plugin("org.jetbrains.kotlin.plugin.spring")
        plugin("org.jetbrains.kotlin.plugin.jpa")
        plugin("org.springframework.boot")
        plugin("io.spring.dependency-management")
    }

    dependencies {
        implementation("org.jetbrains.kotlin:kotlin-reflect")
        implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
        implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
        testImplementation("org.springframework.boot:spring-boot-starter-test")
    }

    tasks.getByName("bootJar") {
        enabled = false
    }

    tasks.getByName("jar") {
        enabled = true
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs = listOf("-Xjsr305=strict")
            jvmTarget = "${project.property("javaVersion")}"
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

}
