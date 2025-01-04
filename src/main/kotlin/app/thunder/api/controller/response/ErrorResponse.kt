package app.thunder.api.controller.response

import com.fasterxml.jackson.annotation.JsonInclude
import java.time.ZonedDateTime

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ErrorResponse(
    val errorCode: String,
    val message: String,
    val path: String,
    val timestamp: ZonedDateTime = ZonedDateTime.now()
)
