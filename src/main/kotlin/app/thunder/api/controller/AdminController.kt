package app.thunder.api.controller

import app.thunder.api.application.AdminService
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Validated
@RequestMapping(value = ["/v1/admin"])
@RestController
class AdminController(
    private val adminService: AdminService,
) {

    @PostMapping("/reset")
    fun postReset(
        @RequestParam(required = true) target: String,
        @RequestParam(required = true) mobileNumber: String,
    ) {
        adminService.reset(target, mobileNumber)
    }

}