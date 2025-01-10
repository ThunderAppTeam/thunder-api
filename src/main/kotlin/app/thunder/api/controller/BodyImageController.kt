package app.thunder.api.controller

import app.thunder.api.application.BodyImageService
import app.thunder.api.controller.response.SuccessResponse
import jakarta.servlet.http.HttpServletRequest
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@Validated
@RequestMapping(value = ["/v1/body/image"])
@RestController
class BodyImageController(
    private val bodyImageService: BodyImageService
) {

    @PostMapping
    fun postBodyImage(
        @RequestParam("file") file: MultipartFile,
        @RequestParam("memberId") memberId: Long,
        servlet: HttpServletRequest
    ): SuccessResponse<Void> {
        bodyImageService.share(file, memberId)
        return SuccessResponse(path = servlet.requestURI)
    }

}