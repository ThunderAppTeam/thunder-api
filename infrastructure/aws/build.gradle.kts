tasks.getByName("bootJar") {
    enabled = false
}

tasks.getByName("jar") {
    enabled = true
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")

    implementation("software.amazon.awssdk:s3:2.30.27")
    implementation("software.amazon.awssdk:rekognition:2.30.27")
}
