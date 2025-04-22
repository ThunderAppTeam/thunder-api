package app.thunder.api.auth

import app.thunder.api.config.AdminProperties
import app.thunder.core.member.domain.Member
import app.thunder.core.member.repository.MemberRepository
import app.thunder.libraries.errors.MemberErrors.INVALID_TOKEN
import app.thunder.libraries.errors.ThunderException
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val tokenManager: TokenManager,
    private val memberRepository: MemberRepository,
    private val adminProperties: AdminProperties,
) : OncePerRequestFilter() {

    companion object {
        private const val BEARER_PREFIX = "bearer "
        const val ADMIN_ROLE = "ADMIN"
    }

    override fun doFilterInternal(request: HttpServletRequest,
                                  response: HttpServletResponse,
                                  filterChain: FilterChain) {
        val authorization: String? = request.getHeader(HttpHeaders.AUTHORIZATION)
        var memberId: Long? = null
        if (authorization != null && authorization.startsWith(BEARER_PREFIX, ignoreCase = true)) {
            val token: String = authorization.substring(7)
            val claims = tokenManager.parse(token)
            memberId = claims.subject.toLongOrNull() ?: throw ThunderException(INVALID_TOKEN)
            requireActiveToken(memberId, claims.issuedAt)
        }

        if (memberId != null && SecurityContextHolder.getContext().authentication == null) {
            val authenticationToken =
                UsernamePasswordAuthenticationToken(memberId, null, authoritiesOf(memberId))
            authenticationToken.details = WebAuthenticationDetailsSource().buildDetails(request)
            SecurityContextHolder.getContext().authentication = authenticationToken
        }

        filterChain.doFilter(request, response)
    }

    private fun requireActiveToken(memberId: Long, issuedAt: Instant) {
        val member = memberRepository.getById(memberId) ?: throw ThunderException(INVALID_TOKEN)
        if (isIssuedBeforeLogout(member, issuedAt)) {
            throw ThunderException(INVALID_TOKEN)
        }
    }

    private fun isIssuedBeforeLogout(member: Member, issuedAt: Instant): Boolean {
        val loggedOutAt = member.loggedOutAt ?: return false
        val loggedOutInstant = loggedOutAt.truncatedTo(ChronoUnit.SECONDS).atZone(ZoneId.systemDefault()).toInstant()
        return issuedAt.isBefore(loggedOutInstant)
    }

    private fun authoritiesOf(memberId: Long): List<GrantedAuthority> {
        if (memberId in adminProperties.memberIds) {
            return listOf(SimpleGrantedAuthority("ROLE_$ADMIN_ROLE"))
        }
        return emptyList()
    }

}
