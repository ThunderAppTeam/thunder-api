package app.thunder.domain.member

interface SmsPort {
    fun sendSms(
        receiverMobileNumber: String,
        message: String,
        isTestMode: Boolean = false,
    )
}
