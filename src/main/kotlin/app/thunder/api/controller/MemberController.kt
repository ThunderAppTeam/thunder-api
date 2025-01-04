package app.thunder.api.controller

import app.thunder.api.application.MemberService
import app.thunder.api.controller.request.PostSignupRequest
import app.thunder.api.controller.request.PostSmsRequest
import app.thunder.api.controller.request.PostSmsVerifyRequest
import app.thunder.api.controller.response.SuccessResponse
import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.*

@RequestMapping(value = ["/v1/member"])
@RestController
class MemberController(
    private val memberService: MemberService
) {

    @PostMapping("/sms")
    fun postSms(@RequestBody request: PostSmsRequest) {
        memberService.sendSms(request.mobileNumber, request.mobileCountry)
    }

    @PostMapping("/sms/verify")
    fun postSmsVerify(@RequestBody request: PostSmsVerifyRequest, servlet: HttpServletRequest): SuccessResponse<Void> {
        memberService.verifySms(request.mobileNumber, request.verificationCode)
        return SuccessResponse(message = "Mobile Verification complete.", path = servlet.requestURI)
    }

    @PostMapping("/signup")
    fun postSignup(@RequestBody request: PostSignupRequest) {
        memberService.signup(request)
    }

    @GetMapping("/nickname/available")
    fun getNicknameAvailable(@RequestParam nickname: String,
                             servlet: HttpServletRequest): SuccessResponse<Void> {
        memberService.isAvailableNickName(nickname)
        return SuccessResponse(message = "The nickname is available.", path = servlet.requestURI)
    }

}