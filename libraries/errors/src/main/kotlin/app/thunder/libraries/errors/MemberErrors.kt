package app.thunder.libraries.errors

import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.CONFLICT
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.HttpStatus.TOO_MANY_REQUESTS
import org.springframework.http.HttpStatus.UNAUTHORIZED

enum class MemberErrors(
    override val httpStatus: HttpStatus,
    override val message: String,
) : ErrorCode {

    NOT_FOUND_MOBILE_VERIFICATION(BAD_REQUEST, "Verification code not sent to this mobile number."),
    EXPIRED_MOBILE_VERIFICATION(BAD_REQUEST, "Verification code is expired."),
    INVALID_MOBILE_VERIFICATION(BAD_REQUEST, "Invalid mobile verification code."),
    TOO_MANY_MOBILE_VERIFICATION(TOO_MANY_REQUESTS, "Mobile verification can only be requested 5 times per day."),
    TOO_MANY_VERIFICATION_ATTEMPTS(
        TOO_MANY_REQUESTS,
        "Too many failed verification attempts. Request a new verification code.",
    ),
    MOBILE_NOT_VERIFIED(FORBIDDEN, "Mobile number is not verified."),
    NICKNAME_DUPLICATED(CONFLICT, "Nickname already exists."),
    MOBILE_NUMBER_DUPLICATED(CONFLICT, "This mobile number is already registered."),

    NOT_FOUND_MEMBER(NOT_FOUND, "Member not found."),
    CANNOT_BLOCK_SELF(BAD_REQUEST, "You cannot block yourself."),
    EXPIRED_TOKEN(UNAUTHORIZED, "The token has expired."),
    INVALID_TOKEN(UNAUTHORIZED, "The token is invalid."),
    FCM_TOKEN_ALREADY_SAVED(CONFLICT, "FCM token has already been saved."),
    ADMIN_ONLY_ACCESS(FORBIDDEN, "Only admin has permission for this request."),
    ;

}
