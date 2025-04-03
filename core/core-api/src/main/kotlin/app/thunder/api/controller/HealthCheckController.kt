package app.thunder.api.controller

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RequestMapping("/health")
@RestController
class HealthCheckController {

    @GetMapping
    fun healthCheck(): ResponseEntity<Void> {
        return ResponseEntity.ok().build()
    }

}