package app.thunder.api.auth

import app.thunder.libraries.errors.MemberErrors.EXPIRED_TOKEN
import app.thunder.libraries.errors.MemberErrors.INVALID_TOKEN
import app.thunder.libraries.errors.ThunderException
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import java.time.Instant
import java.util.Date
import javax.crypto.SecretKey
import org.springframework.stereotype.Component

@Component
class TokenManager(
    val jwtProperties: JwtProperties
) {

    fun generateAccessToken(subject: Any): String {
        return this.generateToken(subject.toString(), jwtProperties.accessTokenExpiration)
    }

    fun generateRefreshToken(subject: Any): String {
        return this.generateToken(subject.toString(), jwtProperties.refreshTokenExpiration)
    }

    private fun generateToken(subject: String, expirationMs: Long): String {
        val expiration = Date(Date().time + expirationMs)
        val token = Jwts.builder()
            .subject(subject)
            .issuedAt(Date())
            .expiration(expiration)
            .signWith(this.getSecretKey())
            .compact()
        return token
    }

    fun parse(token: String): TokenClaims {
        try {
            val payload = Jwts.parser()
                .verifyWith(this.getSecretKey())
                .build()
                .parseSignedClaims(token)
                .payload
            return TokenClaims(subject = payload.subject, issuedAt = payload.issuedAt.toInstant())
        } catch (_: ExpiredJwtException) {
            throw ThunderException(EXPIRED_TOKEN)
        } catch (_: Exception) {
            throw ThunderException(INVALID_TOKEN)
        }
    }

    private fun getSecretKey(): SecretKey {
        return Keys.hmacShaKeyFor(jwtProperties.secretKey.toByteArray())
    }

}


data class TokenClaims(
    val subject: String,
    val issuedAt: Instant,
)
