package app.thunder.api.exception

import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR

enum class ExternalErrors(
    override val httpStatus: HttpStatus,
    override val message: String,
) : ErrorCode {

    NOT_FOUND_MOBILE_NUMBER(BAD_REQUEST, "Not found mobile number."),
    SEND_SMS_API_ERROR(INTERNAL_SERVER_ERROR, "Failed to send SMS due to an error in the external SMS service."),
    ;

}