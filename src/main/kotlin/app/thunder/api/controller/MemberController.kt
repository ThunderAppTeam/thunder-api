package app.thunder.api.controller

import app.thunder.api.application.AuthService
import app.thunder.api.application.MemberService
import app.thunder.api.controller.request.DeleteMemberRequest
import app.thunder.api.controller.request.PostFcmTokenRequest
import app.thunder.api.controller.request.PostLoginResponse
import app.thunder.api.controller.request.PostMemberBlockRequest
import app.thunder.api.controller.request.PostSignupRequest
import app.thunder.api.controller.request.PostSmsRequest
import app.thunder.api.controller.request.PostSmsResetRequest
import app.thunder.api.controller.request.PostSmsVerifyRequest
import app.thunder.api.controller.request.PutMemberSettingsRequest
import app.thunder.api.controller.response.GetMemberDeletionReasonResponse
import app.thunder.api.controller.response.GetMemberInfoResponse
import app.thunder.api.controller.response.GetMemberSettingsResponse
import app.thunder.api.controller.response.PostSignUpResponse
import app.thunder.api.controller.response.SuccessResponse
import app.thunder.api.controller.response.TestSendSmsResponse
import app.thunder.api.domain.member.MemberDeletionReason
import app.thunder.api.func.toKoreaZonedDateTime
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Validated
@RequestMapping(value = ["/v1/member"])
@RestController
class MemberController(
    private val authService: AuthService,
    private val memberService: MemberService,
) {

    @PostMapping("/sms")
    fun postSms(
        @RequestBody @Valid request: PostSmsRequest,
    ): TestSendSmsResponse {
        val verificationCode: String = authService.sendSms(request)
        return TestSendSmsResponse(
            verificationCode.takeIf { request.isTestMode }
        )
    }

    @PostMapping("/sms/verify")
    fun postSmsVerify(
        @RequestBody @Valid request: PostSmsVerifyRequest,
        servlet: HttpServletRequest
    ): SuccessResponse<PostLoginResponse> {
        val memberAccessToken = authService.verifySms(request.deviceId,
                                                      request.mobileNumber,
                                                      request.verificationCode)

        val response = PostLoginResponse(memberId = memberAccessToken.member?.memberId,
                                         memberUuid = memberAccessToken.member?.memberUuid,
                                         age = memberAccessToken.member?.age,
                                         gender = memberAccessToken.member?.gender,
                                         accessToken = memberAccessToken.accessToken)

        return SuccessResponse(message = "Mobile Verification complete.",
                               data = response,
                               path = servlet.requestURI)
    }

    @PostMapping("/sms/reset")
    fun postSmsReset(
        @RequestBody @Valid request: PostSmsResetRequest,
    ) {
        authService.resetSendLimit(request)
    }

    @PostMapping("/signup")
    fun postSignup(
        @RequestBody @Valid request: PostSignupRequest,
    ): PostSignUpResponse {
        val memberAccessToken = authService.signup(request)
        return PostSignUpResponse(memberId = memberAccessToken.member?.memberId,
                                  memberUuid = memberAccessToken.member?.memberUuid,
                                  age = memberAccessToken.member?.age,
                                  gender = memberAccessToken.member?.gender,
                                  accessToken = memberAccessToken.accessToken)
    }

    @GetMapping("/nickname/available")
    fun getNicknameAvailable(
        @RequestParam @NotBlank nickname: String,
        servlet: HttpServletRequest
    ): SuccessResponse<Void> {
        authService.isAvailableNickName(nickname)
        return SuccessResponse(message = "The nickname is available.", path = servlet.requestURI)
    }

    @GetMapping("/info")
    fun getMember(
        @AuthenticationPrincipal memberId: Long
    ): GetMemberInfoResponse {
        val member = memberService.getById(memberId)
        return GetMemberInfoResponse(memberUuid = member.memberUuid,
                                     nickname = member.nickname,
                                     mobileNumber = member.mobileNumber,
                                     registeredAt = member.createdAt.toKoreaZonedDateTime())
    }

    @PostMapping("/block")
    fun postBlockMember(
        @RequestBody @Valid request: PostMemberBlockRequest,
        @AuthenticationPrincipal memberId: Long,
    ) {
        memberService.block(memberId, request.blockedMemberId)
    }

    @GetMapping("/deletion-reason")
    fun getMemberDeletionReasons(
        @RequestParam(defaultValue = "KR") countryCode: String,
    ): List<GetMemberDeletionReasonResponse> {
        return MemberDeletionReason.entries.map {
            when (countryCode) {
                "KR" -> GetMemberDeletionReasonResponse(it.name, it.descriptionKR)
                else -> GetMemberDeletionReasonResponse(it.name, it.descriptionKR)
            }
        }
    }

    @PostMapping("/deletion")
    fun delete(
        @RequestBody @Valid request: DeleteMemberRequest,
        @AuthenticationPrincipal memberId: Long,
    ) {
        memberService.delete(memberId, request.deletionReason, request.otherReason)
    }

    @PutMapping("/logout")
    fun logout(@AuthenticationPrincipal memberId: Long) {
        memberService.logout(memberId)
    }

    @PostMapping("/fcm-token")
    fun postFcmToken(
        @RequestBody @Valid request: PostFcmTokenRequest,
        @AuthenticationPrincipal memberId: Long,
    ) {
        memberService.savedFcmToken(memberId, request.fcmToken)
    }

    @GetMapping("/settings")
    fun getMemberSettings(
        @AuthenticationPrincipal memberId: Long,
    ): GetMemberSettingsResponse {
        val settings = memberService.getSettings(memberId)
        return GetMemberSettingsResponse(
            memberId = settings.memberId,
            reviewCompleteNotify = settings.reviewCompleteNotify,
            reviewRequestNotify = settings.reviewRequestNotify,
            marketingAgreement = settings.marketingAgreement
        )
    }

    @PutMapping("/settings")
    fun putMemberSettings(
        @RequestBody @Valid request: PutMemberSettingsRequest,
        @AuthenticationPrincipal memberId: Long,
    ) {
        memberService.updateSettings(memberId, request)
    }

}