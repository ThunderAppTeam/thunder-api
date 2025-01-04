package app.thunder.api.controller.response

import java.time.ZonedDateTime

data class SuccessResponse<T>(
    val message: String = DEFAULT_SUCCESS_MESSAGE,
    val data: T? = null,
    val timestamp: ZonedDateTime = ZonedDateTime.now()
) {

    companion object {
        const val DEFAULT_SUCCESS_MESSAGE = "Request completed successfully"
    }

}
