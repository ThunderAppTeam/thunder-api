package app.thunder.api.config

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class FirebaseConfig {

    @Bean
    fun firebaseMessaging(): FirebaseMessaging {
        val options = FirebaseOptions.builder()
            .setCredentials(GoogleCredentials.getApplicationDefault())
            .build()
        val firebaseApp = FirebaseApp.initializeApp(options)
        return FirebaseMessaging.getInstance(firebaseApp)
    }

}