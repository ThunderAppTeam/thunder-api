plugins {
    id("thunder.kotlin")
}

pluginManager.withPlugin("org.springframework.boot") {
    tasks.getByName("bootJar") {
        enabled = false
    }
}

tasks.getByName("jar") {
    enabled = true
}
