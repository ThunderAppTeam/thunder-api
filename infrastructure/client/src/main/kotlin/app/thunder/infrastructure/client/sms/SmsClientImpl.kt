package app.thunder.infrastructure.client.sms

import app.thunder.core.member.client.SmsClient
import app.thunder.libraries.errors.ExternalApiException
import app.thunder.libraries.errors.ExternalErrors
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
internal class SmsClientImpl(
    private val aligoHttpClient: AligoHttpClient,
    private val aligoProperties: AligoProperties,
) : SmsClient {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun sendSms(receiverMobileNumber: String, message: String, isTestMode: Boolean) {
        val response = aligoHttpClient.sendSms(
            key = aligoProperties.apiKey,
            userId = aligoProperties.userId,
            sender = aligoProperties.sender,
            receiver = receiverMobileNumber,
            message = message,
            isTestMode = if (isTestMode) "Y" else "N"
        )

        if (response.resultCode < 0) {
            logger.error("SmsClientImpl sendSms() Error :: {}", response)
            if (response.message == "받는이가 설정되지 못하였습니다.") {
                throw ExternalApiException(ExternalErrors.NOT_FOUND_MOBILE_NUMBER)
            }
            throw ExternalApiException(ExternalErrors.SEND_SMS_API_ERROR)
        }
    }

}
