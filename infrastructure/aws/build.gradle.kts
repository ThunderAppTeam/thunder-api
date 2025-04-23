plugins {
    id("thunder.spring")
    id("thunder.library")
}

dependencies {
    implementation(project(":core"))
    implementation("org.springframework.boot:spring-boot-starter-web")

    implementation(libs.awssdk.s3)
    implementation(libs.awssdk.rekognition)

    testImplementation(libs.instancio.junit)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
