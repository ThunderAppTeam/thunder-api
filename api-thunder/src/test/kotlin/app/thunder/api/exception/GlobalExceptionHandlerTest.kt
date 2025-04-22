package app.thunder.api.exception

import app.thunder.api.ThunderApiWebTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

@Import(GlobalExceptionHandlerTest.DuplicateInsertController::class)
class GlobalExceptionHandlerTest : ThunderApiWebTest() {

    @BeforeEach
    fun createMember() {
        insertMember(MEMBER_ID)
    }

    @AfterEach
    fun cleanUp() {
        jdbcTemplate.update("DELETE FROM member_block_relation WHERE member_id = ?", MEMBER_ID)
    }

    @Test
    @DisplayName("JSON 문법이 깨진 요청 본문은 400으로 응답한다")
    fun malformedJson() {
        // given
        val body = """{"a":"""

        // when
        val result = mockMvc.perform(post("/v1/member/sms").contentType(APPLICATION_JSON).content(body))

        // then
        result.andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errorCode").value("INVALID_PARAMETER_VALUE"))
    }

    @Test
    @DisplayName("unique index를 위반하는 중복 insert는 409 DATA_CONFLICT로 응답한다")
    fun duplicateInsert() {
        // given
        val authorization = bearerToken(MEMBER_ID)

        // when
        val result = mockMvc.perform(post("/v1/test/duplicate-block").header(AUTHORIZATION, authorization))

        // then
        result.andExpect(status().isConflict)
            .andExpect(jsonPath("$.errorCode").value("DATA_CONFLICT"))
            .andExpect(jsonPath("$.message").value("The request conflicts with existing data."))
    }

    @Test
    @DisplayName("unique index가 아닌 NOT NULL 위반은 서버 오류로 남겨 409로 감추지 않는다")
    fun notNullViolation() {
        // given
        val authorization = bearerToken(MEMBER_ID)

        // when
        val result = mockMvc.perform(post("/v1/test/null-block").header(AUTHORIZATION, authorization))

        // then
        result.andExpect(status().isInternalServerError)
            .andExpect(jsonPath("$.errorCode").value("UNKNOWN_SERVER_ERROR"))
    }

    @RestController
    class DuplicateInsertController(private val jdbcTemplate: JdbcTemplate) {

        @PostMapping("/v1/test/null-block")
        fun insertNull() {
            jdbcTemplate.update(
                "INSERT INTO member_block_relation (member_id, blocked_member_id, created_at, created_by) VALUES (?, ?, ?, ?)",
                MEMBER_ID, BLOCKED_MEMBER_ID, null, MEMBER_ID,
            )
        }

        @PostMapping("/v1/test/duplicate-block")
        fun insertTwice() {
            repeat(2) {
                jdbcTemplate.update(
                    "INSERT INTO member_block_relation (member_id, blocked_member_id, created_at, created_by) VALUES (?, ?, ?, ?)",
                    MEMBER_ID, BLOCKED_MEMBER_ID, LocalDateTime.now(), MEMBER_ID,
                )
            }
        }
    }

    companion object {
        private const val MEMBER_ID = 9_001L
        private const val BLOCKED_MEMBER_ID = 9_002L
    }

}
