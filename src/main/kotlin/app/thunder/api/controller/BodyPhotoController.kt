package app.thunder.api.controller

import app.thunder.api.application.BodyPhotoService
import app.thunder.api.controller.response.GetBodyPhotoResponse
import app.thunder.api.controller.response.GetBodyPhotoResultResponse
import app.thunder.api.controller.response.PostBodyPhotoResponse
import app.thunder.api.controller.response.SuccessResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.constraints.Positive
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@Validated
@RequestMapping(value = ["/v1/body/photo"])
@RestController
class BodyPhotoController(
    private val bodyPhotoService: BodyPhotoService,
) {

    @GetMapping
    fun getBodyPhoto(
        @AuthenticationPrincipal memberId: Long,
        servlet: HttpServletRequest,
    ): SuccessResponse<List<GetBodyPhotoResponse>> {
        val bodyPhotoList = bodyPhotoService.getAllByMemberId(memberId)
        return SuccessResponse(data = bodyPhotoList, path = servlet.requestURI)
    }

    @GetMapping("/{bodyPhotoId}")
    fun getBodyPhotoDetail(
        @PathVariable @Positive bodyPhotoId: Long,
        @AuthenticationPrincipal memberId: Long,
        servlet: HttpServletRequest,
    ): SuccessResponse<GetBodyPhotoResultResponse> {
        val bodyPhotoResult = bodyPhotoService.getByBodyPhotoId(bodyPhotoId, memberId)
        return SuccessResponse(data = bodyPhotoResult, path = servlet.requestURI)
    }

    @PostMapping
    fun postBodyPhoto(
        @RequestPart("file") file: MultipartFile,
        @AuthenticationPrincipal memberId: Long,
        servlet: HttpServletRequest
    ): SuccessResponse<PostBodyPhotoResponse> {
        val bodyPhoto = bodyPhotoService.upload(file, memberId)
        val response = PostBodyPhotoResponse(bodyPhoto.bodyPhotoId, bodyPhoto.imageUrl)
        return SuccessResponse(data = response, path = servlet.requestURI)
    }

    @DeleteMapping("/{bodyPhotoId}")
    fun deleteBodyPhoto(
        @PathVariable bodyPhotoId: Long,
        @AuthenticationPrincipal memberId: Long,
        servlet: HttpServletRequest
    ): SuccessResponse<Void> {
        bodyPhotoService.deleteByBodyPhotoId(bodyPhotoId, memberId)
        return SuccessResponse(path = servlet.requestURI)
    }

}