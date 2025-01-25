package app.thunder.api.controller

import app.thunder.api.application.BodyService
import app.thunder.api.controller.request.PostBodyReviewRequest
import app.thunder.api.controller.response.PostBodyPhotoResponse
import app.thunder.api.controller.response.PostReviewRefreshResponse
import app.thunder.api.controller.response.SuccessResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import jakarta.validation.constraints.Positive
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Validated
@RequestMapping(value = ["/v1/body/review"])
@RestController
class BodyReviewController(
    private val bodyService: BodyService,
) {

    @PostMapping("/refresh")
    fun getBodyReviewRefresh(
        @RequestParam @Positive refreshCount: Int,
        @AuthenticationPrincipal memberId: Long,
        servlet: HttpServletRequest,
    ): SuccessResponse<List<PostReviewRefreshResponse>> {
        val response = bodyService.refreshReview(memberId, refreshCount)
        return SuccessResponse(data = response, path = servlet.requestURI)
    }

    @PostMapping
    fun postBodyReview(
        @RequestBody @Valid request: PostBodyReviewRequest,
        @AuthenticationPrincipal memberId: Long,
        servlet: HttpServletRequest
    ): SuccessResponse<PostBodyPhotoResponse> {
        bodyService.review(request.bodyPhotoId, memberId, request.score)
        return SuccessResponse(path = servlet.requestURI)
    }

}