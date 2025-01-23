package app.thunder.api.func

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

fun LocalDateTime.toKoreaZonedDateTime(): ZonedDateTime {
    val koreaZone = ZoneId.of("Asia/Seoul")
    return this.toInstant(ZoneOffset.UTC).atZone(koreaZone)
}