package app.thunder.api.exception

import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.HttpStatus.CONFLICT
import org.springframework.http.HttpStatus.FORBIDDEN
import org.springframework.http.HttpStatus.NOT_FOUND

enum class BodyErrors(
    override val httpStatus: HttpStatus,
    override val message: String,
) : ErrorCode {

    UNSUPPORTED_IMAGE_FORMAT(BAD_REQUEST, "Only JPG and PNG formats are allowed for image."),
    NOT_FOUND_BODY_PHOTO(NOT_FOUND, "Body Photo not found."),
    ALREADY_REVIEWED(CONFLICT, "Body Photo has already been reviewed by same member."),
    ALREADY_FLAGGED(CONFLICT, "Body Photo has already been flagged by same member."),
    UPLOADER_OR_ADMIN_ONLY_ACCESS(FORBIDDEN, "Only uploader or admin has permission for this request."),
    BODY_NOT_DETECTED_IN_PHOTO(BAD_REQUEST, "Unable to detect body in image.")
    ;

}