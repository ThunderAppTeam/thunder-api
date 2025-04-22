package app.thunder.api.exception

import app.thunder.libraries.errors.CommonErrors.INVALID_PARAMETER_VALUE
import app.thunder.libraries.errors.CommonErrors.UNKNOWN_SERVER_ERROR
import app.thunder.libraries.errors.ErrorCode
import app.thunder.libraries.errors.ThunderException
import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.FilterConfig
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.http.converter.HttpMessageNotReadableException

class ExceptionHandlingFilter(
    private val errorResponseWriter: ErrorResponseWriter,
) : Filter {

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
        errorResponseWriter.write(request, response, error)
    }

}
