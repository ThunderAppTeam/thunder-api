package app.thunder.api.adapter.sms

import org.springframework.stereotype.Component

@Component
class SmsAdapter(
    private val aligoClient: AligoClient,
    private val aligoProperties: AligoProperties,
) {

    fun sendSms(receiverMobileNumber: String, message: String) {
        aligoClient.sendSms(
            key = aligoProperties.apiKey,
            userId = aligoProperties.userId,
            sender = aligoProperties.sender,
            receiver = receiverMobileNumber,
            message = message,
        )
    }

}