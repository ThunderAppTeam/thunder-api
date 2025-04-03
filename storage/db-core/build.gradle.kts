tasks.getByName("bootJar") {
    enabled = false
}

tasks.getByName("jar") {
    enabled = true
}

dependencies {
    implementation(project(":core:core-domain"))

    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("com.linecorp.kotlin-jdsl:jpql-dsl:3.5.4")
    implementation("com.linecorp.kotlin-jdsl:jpql-render:3.5.4")
    implementation("com.linecorp.kotlin-jdsl:spring-data-jpa-support:3.5.4")

    implementation("net.datafaker:datafaker:2.4.2")
}
