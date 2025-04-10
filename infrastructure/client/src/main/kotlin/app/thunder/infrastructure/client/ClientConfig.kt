package app.thunder.infrastructure.client

import app.thunder.infrastructure.client.sms.AligoClient
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.http.codec.json.Jackson2JsonDecoder
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.support.WebClientAdapter
import org.springframework.web.service.invoker.HttpServiceProxyFactory


@Configuration
class ClientConfig {

    @Bean
    fun aligoClient(objectMapper: ObjectMapper): AligoClient {
        val webClient: WebClient = WebClient.builder()
            .baseUrl("https://apis.aligo.in")
            .codecs { configurer ->
                val jsonDecoder = Jackson2JsonDecoder(objectMapper, MediaType.TEXT_HTML)
                configurer.defaultCodecs().jackson2JsonDecoder(jsonDecoder)
            }.build()
        val adapter: WebClientAdapter = WebClientAdapter.create(webClient)
        val factory = HttpServiceProxyFactory.builderFor(adapter).build()
        return factory.createClient(AligoClient::class.java)
    }

}
