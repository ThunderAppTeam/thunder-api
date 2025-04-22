package app.thunder.api.exception

import app.thunder.api.controller.response.ErrorResponse
import app.thunder.api.controller.response.SuccessResponse
import app.thunder.api.controller.response.SuccessResponse.EmptyResponse
import app.thunder.libraries.errors.CommonErrors.DATA_CONFLICT
import app.thunder.libraries.errors.CommonErrors.INVALID_PARAMETER_VALUE
import app.thunder.libraries.errors.CommonErrors.MISSING_REQUIRED_PARAMETER
import app.thunder.libraries.errors.CommonErrors.UNKNOWN_SERVER_ERROR
import app.thunder.libraries.errors.ExternalApiException
import app.thunder.libraries.errors.ThunderException
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.ConstraintViolationException
import java.sql.SQLException
import org.slf4j.LoggerFactory
import org.springframework.core.MethodParameter
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus.BAD_REQUEST
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice

@RestControllerAdvice
class GlobalExceptionHandler : ResponseBodyAdvice<Any> {
    private final val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val UNIQUE_VIOLATION_SQL_STATE = "23505"
    }

    override fun supports(returnType: MethodParameter, converterType: Class<out HttpMessageConverter<*>>): Boolean {
        return true
    }

    override fun beforeBodyWrite(
        body: Any?,
        returnType: MethodParameter,
        mediaType: MediaType,
        selectedConverterType: Class<out HttpMessageConverter<*>>,
        request: ServerHttpRequest,
        response: ServerHttpResponse
    ): Any? {
        if (body is SuccessResponse<*> || body is ErrorResponse || returnType.method?.name == HttpMethod.OPTIONS.name()) {
            return body
        }

        return SuccessResponse(
            data = body ?: EmptyResponse(),
            path = request.uri.path
        )
    }

    @ExceptionHandler(exception = [
        ConstraintViolationException::class,
        MissingServletRequestParameterException::class,
        MethodArgumentNotValidException::class
    ])
    fun handleInvalidParameterException(
        ex: Exception,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {

        val errorMessage = when (ex) {
            is MethodArgumentNotValidException -> ex.bindingResult.fieldErrors
                .joinToString(", ")
                { "[${it.field}]: ${it.defaultMessage}" }

            is ConstraintViolationException -> ex.constraintViolations
                .joinToString(", ")
                { "[${it.propertyPath.last()}]: ${it.message}" }

            is MissingServletRequestParameterException -> ex.body.detail.toString()

            else -> INVALID_PARAMETER_VALUE.message
        }

        val errorResponse = ErrorResponse(
            errorCode = INVALID_PARAMETER_VALUE.name,
            message = errorMessage,
            path = request.requestURI
        )
        return ResponseEntity.status(BAD_REQUEST).body(errorResponse)
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleMissingParameterException(
        ex: HttpMessageNotReadableException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val mismatchedInput = ex.cause as? MismatchedInputException
        val errorResponse = if (mismatchedInput == null) {
            ErrorResponse(
                errorCode = INVALID_PARAMETER_VALUE.name,
                message = INVALID_PARAMETER_VALUE.message,
                path = request.requestURI
            )
        } else {
            ErrorResponse(
                errorCode = MISSING_REQUIRED_PARAMETER.name,
                message = mismatchedInput.path.firstOrNull()
                    ?.let { "Required parameter '${it.fieldName}' is not present." }
                    ?: MISSING_REQUIRED_PARAMETER.message,
                path = request.requestURI
            )
        }
        return ResponseEntity.status(BAD_REQUEST).body(errorResponse)
    }

    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleDataIntegrityViolationException(
        ex: DataIntegrityViolationException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val isUniqueViolation = generateSequence<Throwable>(ex) { it.cause }
            .filterIsInstance<SQLException>()
            .any { it.sqlState == UNIQUE_VIOLATION_SQL_STATE }
        if (!isUniqueViolation) {
            return handleException(ex, request)
        }
        logger.warn("Data conflict on {}", request.requestURI, ex)

        val errorResponse = ErrorResponse(
            errorCode = DATA_CONFLICT.name,
            message = DATA_CONFLICT.message,
            path = request.requestURI
        )
        return ResponseEntity.status(DATA_CONFLICT.httpStatus).body(errorResponse)
    }

    @ExceptionHandler(ThunderException::class)
    fun handleThunderException(
        ex: ThunderException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            errorCode = (ex.errorCode as Enum<*>).name,
            message = ex.errorCode.message,
            path = request.requestURI,
        )
        return ResponseEntity.status(ex.errorCode.httpStatus).body(errorResponse)
    }

    @ExceptionHandler(ExternalApiException::class)
    fun handleExternalException(
        ex: ExternalApiException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            errorCode = ex.errorCode.name,
            message = ex.errorCode.message,
            path = request.requestURI
        )
        return ResponseEntity.status(ex.errorCode.httpStatus).body(errorResponse)
    }

    @ExceptionHandler(Exception::class)
    fun handleException(
        ex: Exception,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        logger.error("Unhandled exception on {}", request.requestURI, ex)

        val errorResponse = ErrorResponse(
            errorCode = UNKNOWN_SERVER_ERROR.name,
            message = UNKNOWN_SERVER_ERROR.message,
            path = request.requestURI
        )
        return ResponseEntity.status(UNKNOWN_SERVER_ERROR.httpStatus).body(errorResponse)
    }

}
