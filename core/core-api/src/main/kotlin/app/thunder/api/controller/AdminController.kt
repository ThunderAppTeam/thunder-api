package app.thunder.api.controller

import app.thunder.api.application.AdminService
import app.thunder.api.controller.request.PostReleaseUiRequest
import app.thunder.api.controller.response.GetReleaseUiResponse
import app.thunder.api.domain.admin.MobileOs
import jakarta.validation.Valid
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Validated
@RequestMapping(value = ["/v1/admin"])
@RestController
class AdminController(
    private val adminService: AdminService,
) {

    @GetMapping("/release-ui")
    fun getUi(
        @RequestParam(required = true) mobileOs: MobileOs,
        @RequestParam(required = true) appVersion: String,
    ): GetReleaseUiResponse {
        val isRelease = adminService.getReleaseUi(mobileOs, appVersion)
        return GetReleaseUiResponse(isRelease)
    }

    @PostMapping("/release-ui")
    fun postUi(
        @RequestBody(required = true) @Valid request: PostReleaseUiRequest
    ) {
        adminService.createOrUpdateReleaseUi(request.mobileOs, request.appVersion, request.isRelease)
    }

}