plugins {
    id("thunder.spring")
    id("thunder.library")
}

dependencies {
    implementation(project(":core"))

    implementation("org.springframework.boot:spring-boot-starter")
    implementation(libs.firebase.admin)

    testImplementation(libs.instancio.junit)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
