package app.thunder.api.exception

import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.*

enum class BodyErrors(
    override val httpStatus: HttpStatus,
    override val message: String,
) : ErrorCode {

    UNSUPPORTED_IMAGE_FORMAT(BAD_REQUEST, "Only JPG and PNG formats are allowed for image."),
    NOT_FOUND_BODY_PHOTO(NOT_FOUND, "Body Photo not found."),
    NOT_FOUND_REVIEW_ROTATION(NOT_FOUND, "Review Rotation not found."),
    ALREADY_REVIEWED(CONFLICT, "Body Photo has already been reviewed by the member."),
    ;

}