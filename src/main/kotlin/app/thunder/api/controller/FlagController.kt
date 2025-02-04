package app.thunder.api.controller

import app.thunder.api.application.FlagService
import app.thunder.api.controller.request.PostFlagRequest
import app.thunder.api.controller.response.GetFlagReasonResponse
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Validated
@RequestMapping(value = ["/v1/body/flag"])
@RestController
class FlagController(
    private val flagService: FlagService,
) {

    @GetMapping
    fun getBodyReviewFlag(
        @RequestParam(defaultValue = "KR") countryCode: String,
    ): List<GetFlagReasonResponse> {
        return flagService.getAllByCountryCode(countryCode)
    }

    @PostMapping
    fun postBodyReviewFlag(
        @RequestBody @Valid request: PostFlagRequest,
        @AuthenticationPrincipal memberId: Long,
    ) {
        return flagService.flagBodyPhoto(memberId, request.bodyPhotoId, request.flagReason, request.otherReason)
    }

}