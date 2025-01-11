package app.thunder.api.auth

import app.thunder.api.exception.MemberErrors.EXPIRED_TOKEN
import app.thunder.api.exception.MemberErrors.INVALID_TOKEN
import app.thunder.api.exception.ThunderException
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Component
import java.util.*
import javax.crypto.SecretKey

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
        } catch (e: ExpiredJwtException) {
            throw ThunderException(EXPIRED_TOKEN)
        } catch (e: Exception) {
            throw ThunderException(INVALID_TOKEN)
        }
    }

    private fun getSecretKey(): SecretKey {
        return Keys.hmacShaKeyFor(jwtProperties.secretKey.toByteArray())
    }

}