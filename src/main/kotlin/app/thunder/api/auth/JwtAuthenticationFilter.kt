package app.thunder.api.auth

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val tokenManager: TokenManager
) : OncePerRequestFilter() {

    companion object {
        private const val BEARER_PREFIX = "bearer "
    }

    override fun doFilterInternal(request: HttpServletRequest,
                                  response: HttpServletResponse,
                                  filterChain: FilterChain) {
        val authorization: String? = request.getHeader(HttpHeaders.AUTHORIZATION)
        var memberId: Long? = null
        if (authorization != null && authorization.startsWith(BEARER_PREFIX, ignoreCase = true)) {
            val token: String = authorization.substring(7)
            memberId = tokenManager.getSubject(token).toLong()
        }

        if (memberId != null && SecurityContextHolder.getContext().authentication == null) {
            val authenticationToken =
                UsernamePasswordAuthenticationToken(memberId, null, null)
            authenticationToken.details = WebAuthenticationDetailsSource().buildDetails(request)
            SecurityContextHolder.getContext().authentication = authenticationToken
        }

        filterChain.doFilter(request, response)
    }

}
