package app.thunder.api.exception

import app.thunder.api.controller.response.ErrorResponse
import app.thunder.api.exception.CommonErrors.INVALID_PARAMETER_VALUE
import app.thunder.api.exception.CommonErrors.UNKNOWN_SERVER_ERROR
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import jakarta.servlet.*
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.http.converter.HttpMessageNotReadableException
import java.nio.charset.StandardCharsets
import java.time.ZonedDateTime

class ExceptionHandlingFilter : Filter {
    private val objectMapper = ObjectMapper()
        .registerModules(JavaTimeModule())
        .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)

    companion object{
        private val logger = LoggerFactory.getLogger(this::class.java)
    }

    override fun init(filterConfig: FilterConfig?) {

    }

    override fun doFilter(request: ServletRequest,
                          response: ServletResponse,
                          chain: FilterChain) {
        try {
            chain.doFilter(request, response)
        } catch (te: ThunderException) {
            handleException(request as HttpServletRequest, response as HttpServletResponse, te.errorCode)
        } catch (re: HttpMessageNotReadableException) {
            handleException(request as HttpServletRequest, response as HttpServletResponse, INVALID_PARAMETER_VALUE)
        } catch (e: Exception) {
            logger.error("", e)
            handleException(request as HttpServletRequest, response as HttpServletResponse, UNKNOWN_SERVER_ERROR)
        }
    }

    private fun handleException(request: HttpServletRequest,
                                response: HttpServletResponse,
                                error: ErrorCode) {
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