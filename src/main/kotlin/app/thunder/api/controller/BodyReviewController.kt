package app.thunder.api.controller

import app.thunder.api.application.BodyReviewService
import app.thunder.api.controller.request.PostBodyReviewRequest
import app.thunder.api.controller.response.GetReviewableResponse
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
    ): List<GetReviewableResponse> {
        return bodyReviewService.getReviewableBodyPhotoList(memberId, size)
    }

    @Deprecated("replaced by getBodyReview()")
    @PostMapping("/refresh")
    fun getBodyReviewRefresh(
        @RequestParam @Positive refreshCount: Int,
        @AuthenticationPrincipal memberId: Long,
    ): List<GetReviewableResponse> {
        return bodyReviewService.refreshReview(memberId, refreshCount)
    }

    @PostMapping
    fun postBodyReview(
        @RequestBody @Valid request: PostBodyReviewRequest,
        @AuthenticationPrincipal memberId: Long,
    ): GetReviewableResponse? {
        return bodyReviewService.review(request.bodyPhotoId, memberId, request.score)
    }

}