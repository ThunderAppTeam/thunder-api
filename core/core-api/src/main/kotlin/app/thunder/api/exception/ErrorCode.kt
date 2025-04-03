package app.thunder.api.exception

import org.springframework.http.HttpStatus

interface ErrorCode {
    val httpStatus: HttpStatus
    val message: String
}