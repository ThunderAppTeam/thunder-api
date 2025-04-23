package app.thunder.infrastructure.client.sms

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicReference
import org.assertj.core.api.Assertions.assertThat
import org.instancio.Instancio
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.http.codec.json.Jackson2JsonDecoder
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.support.WebClientAdapter
import org.springframework.web.service.invoker.HttpServiceProxyFactory
import reactor.netty.http.client.HttpClient

class AligoHttpClientTest {

    private lateinit var server: HttpServer
    private lateinit var aligoHttpClient: AligoHttpClient
    private val received = AtomicReference<ReceivedRequest>()
    private var responseBody = ""

    @BeforeEach
    fun setUp() {
        server = HttpServer.create(InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0)
        server.createContext("/") { exchange -> respond(exchange) }
        server.start()

        val objectMapper = jacksonObjectMapper()
        val webClient = WebClient.builder()
            .baseUrl("http://${InetAddress.getLoopbackAddress().hostAddress}:${server.address.port}")
            .clientConnector(ReactorClientHttpConnector(HttpClient.create()))
            .codecs { configurer ->
                configurer.defaultCodecs().jackson2JsonDecoder(Jackson2JsonDecoder(objectMapper, MediaType.TEXT_HTML))
            }.build()
        val factory = HttpServiceProxyFactory.builderFor(WebClientAdapter.create(webClient)).build()
        aligoHttpClient = factory.createClient(AligoHttpClient::class.java)
    }

    @AfterEach
    fun tearDown() {
        server.stop(0)
    }

    @Test
    @DisplayName("POST /send/ 로 기본값 일반 모드와 SMS 유형을 포함한 파라미터를 보내고 JSON 응답을 SendSmsResponse로 읽는다")
    fun sendsParametersWithDefaultsAndReadsResponse() {
        // given
        val key = Instancio.create(String::class.java)
        val userId = Instancio.create(String::class.java)
        val sender = Instancio.create(String::class.java)
        val receiver = Instancio.create(String::class.java)
        responseBody = """
            {"result_code":1,"message":"success","msg_id":123456,"success_cnt":1,"error_cnt":0,"msg_type":"SMS"}
        """.trimIndent()

        // when
        val response = aligoHttpClient.sendSms(key, userId, sender, receiver, "인증번호 [123456] 입니다 & 확인")

        // then
        val request = received.get()
        assertThat(request.method).isEqualTo("POST")
        assertThat(request.path).isEqualTo("/send/")
        assertThat(request.accept).contains(MediaType.APPLICATION_JSON_VALUE)
        assertThat(request.parameters).containsExactlyInAnyOrderEntriesOf(
            mapOf(
                "key" to key,
                "user_id" to userId,
                "sender" to sender,
                "receiver" to receiver,
                "msg" to "인증번호 [123456] 입니다 & 확인",
                "testmode_yn" to "N",
                "msg_type" to "SMS",
            )
        )
        assertThat(response.resultCode).isEqualTo(1)
        assertThat(response.message).isEqualTo("success")
        assertThat(response.messageId).isEqualTo(123456)
        assertThat(response.successCnt).isEqualTo(1)
        assertThat(response.errorCount).isEqualTo(0)
        assertThat(response.messageType).isEqualTo("SMS")
    }

    @Test
    @DisplayName("지정한 테스트 모드와 메시지 유형을 그대로 보내고, msg_type이 없는 오류 응답은 messageType을 null로 읽는다")
    fun sendsExplicitModeAndReadsErrorResponseWithoutMessageType() {
        // given
        responseBody = """{"result_code":-101,"message":"error","msg_id":0,"success_cnt":0,"error_cnt":1}"""

        // when
        val response = aligoHttpClient.sendSms("k", "u", "s", "r", "m", "Y", "LMS")

        // then
        val parameters = received.get().parameters
        assertThat(parameters).containsEntry("testmode_yn", "Y").containsEntry("msg_type", "LMS")
        assertThat(response.resultCode).isEqualTo(-101)
        assertThat(response.message).isEqualTo("error")
        assertThat(response.errorCount).isEqualTo(1)
        assertThat(response.messageType).isNull()
    }

    private fun respond(exchange: HttpExchange) {
        val body = exchange.requestBody.readAllBytes().toString(StandardCharsets.UTF_8)
        val query = exchange.requestURI.rawQuery.orEmpty()
        received.set(
            ReceivedRequest(
                method = exchange.requestMethod,
                path = exchange.requestURI.path,
                accept = exchange.requestHeaders.getFirst("Accept").orEmpty(),
                parameters = parseParameters("$query&$body"),
            )
        )
        val bytes = responseBody.toByteArray(StandardCharsets.UTF_8)
        exchange.responseHeaders.add("Content-Type", "text/html;charset=UTF-8")
        exchange.sendResponseHeaders(200, bytes.size.toLong())
        exchange.responseBody.use { it.write(bytes) }
    }

    private fun parseParameters(raw: String): Map<String, String> =
        raw.split("&")
            .filter { it.contains("=") }
            .associate {
                val (name, value) = it.split("=", limit = 2)
                URLDecoder.decode(name, StandardCharsets.UTF_8) to URLDecoder.decode(value, StandardCharsets.UTF_8)
            }

    private data class ReceivedRequest(
        val method: String,
        val path: String,
        val accept: String,
        val parameters: Map<String, String>,
    )

}
