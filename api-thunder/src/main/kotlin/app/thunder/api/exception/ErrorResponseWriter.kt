package app.thunder.api.exception

import app.thunder.api.controller.response.ErrorResponse
import app.thunder.libraries.errors.ErrorCode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.nio.charset.StandardCharsets
import java.time.ZonedDateTime
import org.springframework.http.MediaType

class ErrorResponseWriter {
    private val objectMapper = ObjectMapper()
        .registerModules(JavaTimeModule())
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)

    fun write(request: HttpServletRequest, response: HttpServletResponse, error: ErrorCode) {
        val errorResponse = ErrorResponse(errorCode = (error as Enum<*>).name,
                                          message = error.message,
                                          path = request.requestURI,
                                          timestamp = ZonedDateTime.now())
        response.status = error.httpStatus.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = StandardCharsets.UTF_8.name()
        response.writer.write(objectMapper.writeValueAsString(errorResponse))
    }

}
