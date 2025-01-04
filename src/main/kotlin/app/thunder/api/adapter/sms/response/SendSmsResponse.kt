package app.thunder.api.adapter.sms.response

import com.fasterxml.jackson.annotation.JsonProperty

data class SendSmsResponse(
    @JsonProperty("result_code") val resultCode: Int,
    @JsonProperty("message") val message: String,
    @JsonProperty("msg_id") val messageId: Int,
    @JsonProperty("success_cnt") val successCnt: Int,
    @JsonProperty("error_cnt") val errorCount: Int,
    @JsonProperty("msg_type") val messageType: String,
)
