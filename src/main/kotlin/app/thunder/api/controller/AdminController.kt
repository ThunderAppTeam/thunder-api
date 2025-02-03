package app.thunder.api.controller

import app.thunder.api.application.AdminService
import app.thunder.api.controller.response.SuccessResponse
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
    ): SuccessResponse<Void> {
        adminService.reset(target, mobileNumber)
        return SuccessResponse(path = "/v1/admin/reset")
    }

}