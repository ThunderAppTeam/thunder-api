plugins {
    id("thunder.spring")
    id("thunder.library")
}

dependencies {
    implementation(project(":core"))
    implementation(project(":libraries:errors"))
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation(libs.netty.resolver.dns.native.macos)
}
