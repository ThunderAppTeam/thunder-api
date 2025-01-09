package app.thunder.api.exception

import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR

enum class CommonErrors(
    override val httpStatus: HttpStatus,
    override val message: String,
) : ErrorCode {

    UNKNOWN_SERVER_ERROR(INTERNAL_SERVER_ERROR, "An unknown error occurred on the server."),
    MISSING_REQUIRED_PARAMETER(BAD_REQUEST, "Invalid request body format."),
    INVALID_PARAMETER_VALUE(BAD_REQUEST, "Invalid request body format."),
    ;

}