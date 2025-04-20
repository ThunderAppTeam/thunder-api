package app.thunder.core.member.client

interface SmsClient {
    fun sendSms(
        receiverMobileNumber: String,
        message: String,
        isTestMode: Boolean = false,
    )
}
