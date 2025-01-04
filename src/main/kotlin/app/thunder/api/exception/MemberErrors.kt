package app.thunder.api.exception

import org.springframework.http.HttpStatus

enum class MemberErrors(
    val httpStatus: HttpStatus,
    val message: String,
) {

    NOT_FOUND_MOBILE_VERIFICATION(HttpStatus.BAD_REQUEST, "Verification code not sent to this mobile number."),
    INVALID_MOBILE_VERIFICATION(HttpStatus.BAD_REQUEST, "Invalid mobile verification code."),
    TOO_MANY_MOBILE_VERIFICATION(HttpStatus.TOO_MANY_REQUESTS, "Mobile verification can only be requested 5 times per day."),
    NICKNAME_DUPLICATED(HttpStatus.CONFLICT, "Nickname already exists."),
    ;

}