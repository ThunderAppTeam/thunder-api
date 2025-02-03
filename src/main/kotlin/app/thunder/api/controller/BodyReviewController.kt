package app.thunder.api.controller

import app.thunder.api.application.BodyReviewService
import app.thunder.api.controller.request.PostBodyReviewRequest
import app.thunder.api.controller.response.GetReviewableResponse
import app.thunder.api.controller.response.SuccessResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import jakarta.validation.constraints.Positive
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Validated
@RequestMapping(value = ["/v1/body/review"])
@RestController
class BodyReviewController(
    private val bodyReviewService: BodyReviewService,
) {

    @GetMapping
    fun getBodyReview(
        @RequestParam(defaultValue = "5") size: Int = 5,
        @AuthenticationPrincipal memberId: Long,
        servlet: HttpServletRequest,
    ): SuccessResponse<List<GetReviewableResponse>> {
        val response = bodyReviewService.getReviewableBodyPhotoList(memberId, size)
        return SuccessResponse(data = response, path = servlet.requestURI)
    }

    @Deprecated("replaced by getBodyReview()")
    @PostMapping("/refresh")
    fun getBodyReviewRefresh(
        @RequestParam @Positive refreshCount: Int,
        @AuthenticationPrincipal memberId: Long,
        servlet: HttpServletRequest,
    ): SuccessResponse<List<GetReviewableResponse>> {
        val response = bodyReviewService.refreshReview(memberId, refreshCount)
        return SuccessResponse(data = response, path = servlet.requestURI)
    }

    @PostMapping
    fun postBodyReview(
        @RequestBody @Valid request: PostBodyReviewRequest,
        @AuthenticationPrincipal memberId: Long,
        servlet: HttpServletRequest
    ): SuccessResponse<GetReviewableResponse> {
        val response = bodyReviewService.review(request.bodyPhotoId, memberId, request.score)
        return SuccessResponse(data = response, path = servlet.requestURI)
    }

}