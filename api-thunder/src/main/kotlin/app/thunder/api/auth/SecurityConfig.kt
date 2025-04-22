package app.thunder.api.auth

import app.thunder.api.auth.JwtAuthenticationFilter.Companion.ADMIN_ROLE
import app.thunder.api.exception.ErrorResponseWriter
import app.thunder.api.exception.ExceptionHandlingFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@EnableWebSecurity
@Configuration
class SecurityConfig {

    @Bean
    fun filterChain(http: HttpSecurity, jwtAuthenticationFilter: JwtAuthenticationFilter): SecurityFilterChain {
        val errorResponseWriter = ErrorResponseWriter()
        http.csrf { obj: CsrfConfigurer<HttpSecurity> -> obj.disable() }
            .formLogin { it.disable() }
            .httpBasic { it.disable() }
            .authorizeHttpRequests { auth ->
                auth.requestMatchers(HttpMethod.GET, "/v1/admin/release-ui").permitAll()
                auth.requestMatchers("/v1/admin/**").hasRole(ADMIN_ROLE)
                    .requestMatchers(HttpMethod.POST, "/v1/member/sms/reset").hasRole(ADMIN_ROLE)
                    .requestMatchers("/v1/member/sms").permitAll()
                    .requestMatchers("/v1/member/sms/verify").permitAll()
                    .requestMatchers("/v1/member/signup").permitAll()
                    .requestMatchers("/v1/member/nickname/**").permitAll()
                    .requestMatchers("/health").permitAll()
                    .anyRequest().authenticated()
            }
            .exceptionHandling {
                it.authenticationEntryPoint(JwtAuthenticationEntryPoint(errorResponseWriter))
                    .accessDeniedHandler(AdminAccessDeniedHandler(errorResponseWriter))
            }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
            .addFilterBefore(ExceptionHandlingFilter(errorResponseWriter), JwtAuthenticationFilter::class.java)
        return http.build()
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder()
    }

    @Bean
    fun authenticationManager(config: AuthenticationConfiguration): AuthenticationManager {
        return config.authenticationManager
    }

}