package app.thunder.api.controller

import app.thunder.api.application.MemberService
import app.thunder.api.auth.TokenManager
import app.thunder.api.controller.request.PostLoginResponse
import app.thunder.api.controller.request.PostMemberBlockRequest
import app.thunder.api.controller.request.PostSignupRequest
import app.thunder.api.controller.request.PostSmsRequest
import app.thunder.api.controller.request.PostSmsResetRequest
import app.thunder.api.controller.request.PostSmsVerifyRequest
import app.thunder.api.controller.response.PostSignUpResponse
import app.thunder.api.controller.response.SuccessResponse
import app.thunder.api.controller.response.TestSendSmsResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Validated
@RequestMapping(value = ["/v1/member"])
@RestController
class MemberController(
    private val memberService: MemberService,
    private val tokenManager: TokenManager
) {

    @PostMapping("/sms")
    fun postSms(
        @RequestBody @Valid request: PostSmsRequest,
        servlet: HttpServletRequest
    ): SuccessResponse<TestSendSmsResponse> {
        val verificationCode: String = memberService.sendSms(request)
        val response = TestSendSmsResponse(
            verificationCode.takeIf { request.isTestMode }
        )
        return SuccessResponse(path = servlet.requestURI, data = response)
    }

    @PostMapping("/sms/verify")
    fun postSmsVerify(
        @RequestBody @Valid request: PostSmsVerifyRequest,
        servlet: HttpServletRequest
    ): SuccessResponse<PostLoginResponse> {
        val accessToken = memberService.verifySms(request.deviceId, request.mobileNumber, request.verificationCode)
        return SuccessResponse(message = "Mobile Verification complete.",
                               data = PostLoginResponse(accessToken),
                               path = servlet.requestURI)
    }

    @PostMapping("/sms/reset")
    fun postSmsReset(
        @RequestBody @Valid request: PostSmsResetRequest,
        servlet: HttpServletRequest
    ): SuccessResponse<Void> {
        memberService.resetSendLimit(request)
        return SuccessResponse(path = servlet.requestURI)
    }

    @PostMapping("/signup")
    fun postSignup(
        @RequestBody @Valid request: PostSignupRequest,
        servlet: HttpServletRequest
    ): SuccessResponse<PostSignUpResponse> {
        val member = memberService.signup(request)
        val accessToken = tokenManager.generateAccessToken(member.memberId)
        val response = PostSignUpResponse(memberId = member.memberId, accessToken = accessToken)
        return SuccessResponse(data = response, path = servlet.requestURI)
    }

    @GetMapping("/nickname/available")
    fun getNicknameAvailable(
        @RequestParam @NotBlank nickname: String,
        servlet: HttpServletRequest
    ): SuccessResponse<Void> {
        memberService.isAvailableNickName(nickname)
        return SuccessResponse(message = "The nickname is available.", path = servlet.requestURI)
    }

    @PostMapping("/block")
    fun postBlockMember(
        @RequestBody @Valid request: PostMemberBlockRequest,
        @AuthenticationPrincipal memberId: Long,
        servlet: HttpServletRequest
    ): SuccessResponse<Void> {
        memberService.block(memberId, request.blockedMemberId)
        return SuccessResponse(path = servlet.requestURI)
    }

}