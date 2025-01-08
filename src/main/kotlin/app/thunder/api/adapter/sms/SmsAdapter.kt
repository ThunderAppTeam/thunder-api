package app.thunder.api.adapter.sms

import app.thunder.api.exception.ExternalApiException
import app.thunder.api.exception.ExternalErrors
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class SmsAdapter(
    private val aligoClient: AligoClient,
    private val aligoProperties: AligoProperties,
) {

    companion object {
        private val logger = LoggerFactory.getLogger(this::class.java)
    }

    fun sendSms(receiverMobileNumber: String, message: String, isTestMode: Boolean = false) {
        val response = aligoClient.sendSms(
            key = aligoProperties.apiKey,
            userId = aligoProperties.userId,
            sender = aligoProperties.sender,
            receiver = receiverMobileNumber,
            message = message,
            isTestMode = if (isTestMode) "Y" else "N"
        )

        if (response.resultCode < 0) {
            logger.error("SmsAdapter sendSms() Error :: {}", response)
            if (response.message == "받는이가 설정되지 못하였습니다.") {
                throw ExternalApiException(ExternalErrors.NOT_FOUND_MOBILE_NUMBER)
            }
            throw ExternalApiException(ExternalErrors.SEND_SMS_API_ERROR)
        }
    }

}