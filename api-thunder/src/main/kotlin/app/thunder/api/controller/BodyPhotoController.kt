package app.thunder.api.controller

import app.thunder.api.application.BodyPhotoService
import app.thunder.api.controller.response.GetBodyPhotoResponse
import app.thunder.api.controller.response.GetBodyPhotoResultResponse
import app.thunder.api.controller.response.PostBodyPhotoResponse
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
    ): List<GetBodyPhotoResponse> {
        return bodyPhotoService.getAllByMemberId(memberId)
    }

    @GetMapping("/{bodyPhotoId}")
    fun getBodyPhotoDetail(
        @PathVariable @Positive bodyPhotoId: Long,
        @AuthenticationPrincipal memberId: Long,
    ): GetBodyPhotoResultResponse {
        return bodyPhotoService.getByBodyPhotoId(bodyPhotoId, memberId)
    }

    @PostMapping
    fun postBodyPhoto(
        @RequestPart("file") file: MultipartFile,
        @AuthenticationPrincipal memberId: Long,
    ): PostBodyPhotoResponse {
        val bodyPhoto = bodyPhotoService.upload(file, memberId)
        return PostBodyPhotoResponse(bodyPhoto.bodyPhotoId, bodyPhoto.imageUrl)
    }

    @DeleteMapping("/{bodyPhotoId}")
    fun deleteBodyPhoto(
        @PathVariable bodyPhotoId: Long,
        @AuthenticationPrincipal memberId: Long,
    ) {
        bodyPhotoService.deleteByBodyPhotoId(bodyPhotoId, memberId)
    }

}