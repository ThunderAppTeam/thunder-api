package app.thunder.api.controller.response

import com.fasterxml.jackson.annotation.JsonInclude
import java.time.ZonedDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
data class SuccessResponse<T>(
    val message: String = DEFAULT_SUCCESS_MESSAGE,
    val data: T = EmptyResponse() as T,
    val path: String,
    val timestamp: ZonedDateTime = ZonedDateTime.now()
) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class EmptyResponse(
        val empty: Nothing? = null
    )

    companion object {
        const val DEFAULT_SUCCESS_MESSAGE = "Request completed successfully"
    }

}
