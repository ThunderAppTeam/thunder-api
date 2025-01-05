package app.thunder.api.exception

import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.*

enum class MemberErrors(
    override val httpStatus: HttpStatus,
    override val message: String,
) : ErrorCode {

    NOT_FOUND_MOBILE_VERIFICATION(BAD_REQUEST, "Verification code not sent to this mobile number."),
    EXPIRED_MOBILE_VERIFICATION(BAD_REQUEST, "Verification code is expired."),
    INVALID_MOBILE_VERIFICATION(BAD_REQUEST, "Invalid mobile verification code."),
    TOO_MANY_MOBILE_VERIFICATION(TOO_MANY_REQUESTS, "Mobile verification can only be requested 5 times per day."),
    NICKNAME_DUPLICATED(CONFLICT, "Nickname already exists."),
    ;

}