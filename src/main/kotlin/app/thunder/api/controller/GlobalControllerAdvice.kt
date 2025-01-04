package app.thunder.api.controller

import app.thunder.api.controller.response.ErrorResponse
import app.thunder.api.controller.response.SuccessResponse
import app.thunder.api.exception.ThunderException
import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.MethodParameter
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice

@RestControllerAdvice
class GlobalControllerAdvice : ResponseBodyAdvice<Any> {

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
            data = body,
            path = request.uri.path
        )
    }

    @ExceptionHandler(ThunderException::class)
    fun handleThunderException(
        ex: ThunderException,
        request: HttpServletRequest
    ): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            errorCode = ex.errorCode.name,
            message = ex.errorCode.message,
            path = request.requestURI
        )

        return ResponseEntity.status(ex.errorCode.httpStatus).body(errorResponse)
    }

}