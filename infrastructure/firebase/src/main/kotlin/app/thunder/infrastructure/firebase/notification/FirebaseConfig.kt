package app.thunder.infrastructure.firebase.notification

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource

@Configuration
class FirebaseConfig {

    @Bean
    fun firebaseMessaging(properties: FirebaseProperties): FirebaseMessaging {
        val accountPath = ClassPathResource(properties.accountPath)
        val credentials = GoogleCredentials.fromStream(accountPath.inputStream)

        val options = FirebaseOptions.builder()
            .setCredentials(credentials)
            .build()
        val firebaseApp = FirebaseApp.initializeApp(options)
        return FirebaseMessaging.getInstance(firebaseApp)
    }

}
