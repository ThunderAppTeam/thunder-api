package app.thunder.api.exception

import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.*

enum class BodyErrors(
    override val httpStatus: HttpStatus,
    override val message: String,
) : ErrorCode {

    UNSUPPORTED_IMAGE_FORMAT(BAD_REQUEST, "Only JPG and PNG formats are allowed for image."),
    ;

}