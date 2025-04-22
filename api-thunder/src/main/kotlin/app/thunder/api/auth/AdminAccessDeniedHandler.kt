package app.thunder.api.auth

import app.thunder.api.exception.ErrorResponseWriter
import app.thunder.libraries.errors.MemberErrors.ADMIN_ONLY_ACCESS
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.web.access.AccessDeniedHandler

class AdminAccessDeniedHandler(
    private val errorResponseWriter: ErrorResponseWriter,
) : AccessDeniedHandler {

    override fun handle(request: HttpServletRequest,
                        response: HttpServletResponse,
                        accessDeniedException: AccessDeniedException) {
        errorResponseWriter.write(request, response, ADMIN_ONLY_ACCESS)
    }

}
