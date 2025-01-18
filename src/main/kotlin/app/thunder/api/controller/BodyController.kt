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
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@Validated
@RequestMapping(value = ["/v1/body"])
@RestController
class BodyController(
    private val bodyService: BodyService
) {

    @PostMapping("/review/refresh")
    fun getBodyReview(
        @RequestParam @Positive refreshCount: Int,
        @AuthenticationPrincipal memberId: Long,
        servlet: HttpServletRequest,
    ): SuccessResponse<List<PostReviewRefreshResponse>> {
        val response = bodyService.refreshReview(memberId, refreshCount)
        return SuccessResponse(data = response, path = servlet.requestURI)
    }

    @PostMapping("/photo")
    fun postBodyImage(
        @RequestPart("file") file: MultipartFile,
        @AuthenticationPrincipal memberId: Long,
        servlet: HttpServletRequest
    ): SuccessResponse<PostBodyPhotoResponse> {
        val bodyPhoto = bodyService.upload(file, memberId)
        val response = PostBodyPhotoResponse(bodyPhoto.bodyPhotoId, bodyPhoto.imageUrl)
        return SuccessResponse(data = response, path = servlet.requestURI)
    }

    @PostMapping("/review")
    fun postBodyReview(
        @RequestBody @Valid request: PostBodyReviewRequest,
        @AuthenticationPrincipal memberId: Long,
        servlet: HttpServletRequest
    ): SuccessResponse<PostBodyPhotoResponse> {
        bodyService.review(request.bodyPhotoId, memberId, request.score)
        return SuccessResponse(path = servlet.requestURI)
    }

}