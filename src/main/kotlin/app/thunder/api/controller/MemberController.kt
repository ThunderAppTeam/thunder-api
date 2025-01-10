package app.thunder.api.controller

import app.thunder.api.application.MemberService
import app.thunder.api.controller.request.PostSignupRequest
import app.thunder.api.controller.request.PostSmsRequest
import app.thunder.api.controller.request.PostSmsVerifyRequest
import app.thunder.api.controller.response.SuccessResponse
import app.thunder.api.controller.response.TestSendSmsResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.*

@Validated
@RequestMapping(value = ["/v1/member"])
@RestController
class MemberController(
    private val memberService: MemberService
) {

    @PostMapping("/sms")
    fun postSms(@RequestBody @Valid request: PostSmsRequest,
                servlet: HttpServletRequest): SuccessResponse<TestSendSmsResponse> {
        val verificationCode: String = memberService.sendSms(request)
        var data: TestSendSmsResponse? = null
        if (request.isTestMode) {
            data = TestSendSmsResponse(verificationCode)
        }
        return SuccessResponse(path = servlet.requestURI, data = data)
    }

    @PostMapping("/sms/verify")
    fun postSmsVerify(@RequestBody @Valid request: PostSmsVerifyRequest,
                      servlet: HttpServletRequest): SuccessResponse<Void> {
        memberService.verifySms(request.deviceId, request.mobileNumber, request.verificationCode)
        return SuccessResponse(message = "Mobile Verification complete.", path = servlet.requestURI)
    }

    @PostMapping("/signup")
    fun postSignup(@RequestBody @Valid request: PostSignupRequest,
                   servlet: HttpServletRequest): SuccessResponse<Void> {
        memberService.signup(request)
        return SuccessResponse(path = servlet.requestURI)
    }

    @GetMapping("/nickname/available")
    fun getNicknameAvailable(@RequestParam @NotBlank nickname: String,
                             servlet: HttpServletRequest): SuccessResponse<Void> {
        memberService.isAvailableNickName(nickname)
        return SuccessResponse(message = "The nickname is available.", path = servlet.requestURI)
    }

}