package app.thunder.api.auth

import app.thunder.shared.errors.MemberErrors.EXPIRED_TOKEN
import app.thunder.shared.errors.MemberErrors.INVALID_TOKEN
import app.thunder.shared.errors.ThunderException
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
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

    fun getSubject(token: String): String {
        try {
            return Jwts.parser()
                .verifyWith(this.getSecretKey())
                .build()
                .parseSignedClaims(token)
                .payload
                .subject
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
