package app.thunder.api.controller

import app.thunder.api.controller.request.PostSignupRequest
import app.thunder.api.controller.request.PostSmsRequest
import app.thunder.api.controller.request.PostSmsVerifyRequest
import org.springframework.web.bind.annotation.*

@RequestMapping(value = ["/v1/member"])
@RestController
class MemberController {

    @PostMapping("/sms")
    fun postSms(@RequestBody request: PostSmsRequest) {

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