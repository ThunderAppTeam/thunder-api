package app.thunder.infrastructure.client

import app.thunder.infrastructure.client.sms.AligoHttpClient
import com.fasterxml.jackson.databind.ObjectMapper
import io.netty.channel.ChannelOption
import java.time.Duration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.http.codec.json.Jackson2JsonDecoder
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.support.WebClientAdapter
import org.springframework.web.service.invoker.HttpServiceProxyFactory
import reactor.netty.http.client.HttpClient


@Configuration
class ClientConfig {

    companion object {
        private const val CONNECT_TIMEOUT_MILLIS = 3_000
        private val RESPONSE_TIMEOUT = Duration.ofSeconds(5)
    }

    @Bean
    fun aligoHttpClient(objectMapper: ObjectMapper): AligoHttpClient {
        val httpClient = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_TIMEOUT_MILLIS)
            .responseTimeout(RESPONSE_TIMEOUT)
        val webClient: WebClient = WebClient.builder()
            .baseUrl("https://apis.aligo.in")
            .clientConnector(ReactorClientHttpConnector(httpClient))
            .codecs { configurer ->
                val jsonDecoder = Jackson2JsonDecoder(objectMapper, MediaType.TEXT_HTML)
                configurer.defaultCodecs().jackson2JsonDecoder(jsonDecoder)
            }.build()
        val adapter: WebClientAdapter = WebClientAdapter.create(webClient)
        val factory = HttpServiceProxyFactory.builderFor(adapter).build()
        return factory.createClient(AligoHttpClient::class.java)
    }

}
