package app.thunder.api

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ThunderApiApplication

fun main(args: Array<String>) {
    runApplication<ThunderApiApplication>(*args)
}
