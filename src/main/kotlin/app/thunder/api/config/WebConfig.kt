package app.thunder.api.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.servlet.i18n.SessionLocaleResolver
import java.util.*

@Configuration
class WebConfig : WebMvcConfigurer {

    @Bean
    fun localeResolver(): SessionLocaleResolver {
        val resolver = SessionLocaleResolver()
        resolver.setDefaultLocale(Locale.ENGLISH)
        return resolver
    }

}