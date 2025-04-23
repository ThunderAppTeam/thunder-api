package app.thunder.api.exception

import app.thunder.libraries.errors.MemberErrors.NOT_FOUND_MEMBER
import app.thunder.libraries.errors.ThunderException
import jakarta.servlet.FilterChain
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.mock.http.MockHttpInputMessage
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

class ExceptionHandlingFilterTest {

    private val filter = ExceptionHandlingFilter(ErrorResponseWriter())
    private val request = MockHttpServletRequest("GET", "/v1/anything")
    private val response = MockHttpServletResponse()
    private val chain = mock(FilterChain::class.java)

    @Test
    @DisplayName("예외 없이 끝난 요청은 다음 필터로 넘기고 응답을 건드리지 않는다")
    fun passesThrough() {
        // given
        filter.init(null)

        // when
        filter.doFilter(request, response, chain)

        // then
        verify(chain).doFilter(request, response)
        assertThat(response.status).isEqualTo(200)
        assertThat(response.contentAsString).isEmpty()
    }

    @Test
    @DisplayName("필터 체인에서 ThunderException이 나오면 그 오류 코드의 상태와 본문으로 응답한다")
    fun thunderException() {
        // given
        doThrow(ThunderException(NOT_FOUND_MEMBER)).`when`(chain).doFilter(request, response)

        // when
        filter.doFilter(request, response, chain)

        // then
        assertThat(response.status).isEqualTo(404)
        assertThat(response.contentType).startsWith("application/json")
        assertThat(response.contentAsString)
            .contains("\"errorCode\":\"NOT_FOUND_MEMBER\"")
            .contains("\"message\":\"Member not found.\"")
            .contains("\"path\":\"/v1/anything\"")
    }

    @Test
    @DisplayName("필터 체인에서 요청 본문을 읽지 못하면 400 INVALID_PARAMETER_VALUE로 응답한다")
    fun unreadableRequestBody() {
        // given
        val unreadable = HttpMessageNotReadableException("unreadable", MockHttpInputMessage(ByteArray(0)))
        doThrow(unreadable).`when`(chain).doFilter(request, response)

        // when
        filter.doFilter(request, response, chain)

        // then
        assertThat(response.status).isEqualTo(400)
        assertThat(response.contentAsString)
            .contains("\"errorCode\":\"INVALID_PARAMETER_VALUE\"")
            .contains("\"path\":\"/v1/anything\"")
    }

    @Test
    @DisplayName("필터 체인에서 알 수 없는 예외가 나오면 500 UNKNOWN_SERVER_ERROR로 응답하고 예외 내용은 노출하지 않는다")
    fun unexpectedException() {
        // given
        doThrow(IllegalStateException("secret internal detail")).`when`(chain).doFilter(request, response)

        // when
        filter.doFilter(request, response, chain)

        // then
        assertThat(response.status).isEqualTo(500)
        assertThat(response.contentAsString)
            .contains("\"errorCode\":\"UNKNOWN_SERVER_ERROR\"")
            .doesNotContain("secret internal detail")
    }

}
