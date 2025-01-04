package app.thunder.api.controller

import app.thunder.api.application.MemberService
import app.thunder.api.controller.request.PostSignupRequest
import app.thunder.api.controller.request.PostSmsRequest
import app.thunder.api.controller.request.PostSmsVerifyRequest
import org.springframework.web.bind.annotation.*

@RequestMapping(value = ["/v1/member"])
@RestController
class MemberController(
  private val memberService: MemberService
) {

    @PostMapping("/sms")
    fun postSms(@RequestBody request: PostSmsRequest) {
        memberService.sendSms(request.mobileNumber)
    }

    @PostMapping("/sms/verify")
    fun postSmsVerify(@RequestBody request: PostSmsVerifyRequest) {

    }

    @PostMapping("/signup")
    fun postSignup(@RequestBody request: PostSignupRequest) {

    }

    @GetMapping("/nickname/available")
    fun getNicknameAvailable(@RequestParam nickname: String) {

    }

}