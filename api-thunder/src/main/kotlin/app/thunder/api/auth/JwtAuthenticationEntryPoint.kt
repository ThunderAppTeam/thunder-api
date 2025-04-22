package app.thunder.api.auth

import app.thunder.api.exception.ErrorResponseWriter
import app.thunder.libraries.errors.MemberErrors.INVALID_TOKEN
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint

class JwtAuthenticationEntryPoint(
    private val errorResponseWriter: ErrorResponseWriter,
) : AuthenticationEntryPoint {

    override fun commence(request: HttpServletRequest,
                          response: HttpServletResponse,
                          authException: AuthenticationException) {
        errorResponseWriter.write(request, response, INVALID_TOKEN)
    }

}
