package app.thunder.infrastructure.client.sms

import app.thunder.infrastructure.client.sms.response.SendSmsResponse
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.service.annotation.HttpExchange
import org.springframework.web.service.annotation.PostExchange

@HttpExchange(accept = [APPLICATION_JSON_VALUE])
interface AligoClient {

    @PostExchange("/send/")
    fun sendSms(
        @RequestParam("key") key: String,
        @RequestParam("user_id") userId: String,
        @RequestParam("sender") sender: String,
        @RequestParam("receiver") receiver: String,
        @RequestParam("msg") message: String,
        @RequestParam("testmode_yn") isTestMode: String? = "N",
        @RequestParam("msg_type") messageType: String? = "SMS",
    ): SendSmsResponse

}
