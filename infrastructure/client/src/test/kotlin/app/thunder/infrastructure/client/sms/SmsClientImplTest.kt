package app.thunder.infrastructure.client.sms

import app.thunder.infrastructure.client.sms.response.SendSmsResponse
import app.thunder.libraries.errors.ExternalApiException
import app.thunder.libraries.errors.ExternalErrors
import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.instancio.Instancio
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.slf4j.LoggerFactory

class SmsClientImplTest {

    private lateinit var aligoHttpClient: AligoHttpClient
    private lateinit var properties: AligoProperties
    private lateinit var client: SmsClientImpl
    private lateinit var logAppender: ListAppender<ILoggingEvent>
    private var originalLogLevel: Level? = null
    private val logger = LoggerFactory.getLogger(SmsClientImpl::class.java) as Logger

    @BeforeEach
    fun setUp() {
        aligoHttpClient = mock(AligoHttpClient::class.java)
        properties = Instancio.create(AligoProperties::class.java)
        client = SmsClientImpl(aligoHttpClient, properties)
        logAppender = ListAppender<ILoggingEvent>().apply { start() }
        logger.addAppender(logAppender)
        originalLogLevel = logger.level
        logger.level = Level.ALL
    }

    @AfterEach
    fun tearDown() {
        logger.detachAppender(logAppender)
        logger.level = originalLogLevel
    }

    @Test
    @DisplayName("설정의 인증 정보와 수신 번호, 내용으로 알리고에 일반 모드 SMS 발송을 요청한다")
    fun sendsSmsInNormalMode() {
        // given
        val receiver = Instancio.create(String::class.java)
        val message = Instancio.create(String::class.java)
        stubResponse(receiver, message, "N", response(resultCode = 1))

        // when
        client.sendSms(receiver, message, false)

        // then
        verify(aligoHttpClient).sendSms(
            properties.apiKey, properties.userId, properties.sender, receiver, message, "N", "SMS",
        )
    }

    @Test
    @DisplayName("테스트 모드이면 testmode_yn 값을 Y로 요청한다")
    fun sendsSmsInTestMode() {
        // given
        val receiver = Instancio.create(String::class.java)
        val message = Instancio.create(String::class.java)
        stubResponse(receiver, message, "Y", response(resultCode = 1))

        // when
        client.sendSms(receiver, message, true)

        // then
        verify(aligoHttpClient).sendSms(
            properties.apiKey, properties.userId, properties.sender, receiver, message, "Y", "SMS",
        )
    }

    @Test
    @DisplayName("결과 코드가 0이면 오류로 보지 않고 예외 없이 끝난다")
    fun zeroResultCodeIsNotAnError() {
        // given
        val receiver = Instancio.create(String::class.java)
        val message = Instancio.create(String::class.java)
        stubResponse(receiver, message, "N", response(resultCode = 0))

        // when
        val thrown = catchThrowable { client.sendSms(receiver, message, false) }

        // then
        assertThat(thrown).isNull()
        assertThat(logAppender.list).isEmpty()
    }

    @Test
    @DisplayName("받는이가 설정되지 못했다는 오류 응답이면 휴대폰 번호 없음 예외를 던진다")
    fun throwsNotFoundMobileNumber() {
        // given
        val receiver = Instancio.create(String::class.java)
        val message = Instancio.create(String::class.java)
        stubResponse(receiver, message, "N", response(resultCode = -101, message = "받는이가 설정되지 못하였습니다."))

        // when
        val thrown = catchThrowable { client.sendSms(receiver, message, false) }

        // then
        assertThat(thrown).isInstanceOf(ExternalApiException::class.java)
        val errorCode = (thrown as ExternalApiException).errorCode
        assertThat(errorCode).isEqualTo(ExternalErrors.NOT_FOUND_MOBILE_NUMBER)
        assertThat(errorCode.httpStatus).isEqualTo(HttpStatus.BAD_REQUEST)
        assertThat(errorCode.message).isEqualTo("Not found mobile number.")
    }

    @Test
    @DisplayName("그 밖의 오류 응답이면 오류를 기록하고 SMS 발송 API 오류 예외를 던진다")
    fun throwsSendSmsApiErrorAndLogs() {
        // given
        val receiver = Instancio.create(String::class.java)
        val message = Instancio.create(String::class.java)
        val errorResponse = response(resultCode = -999, message = Instancio.create(String::class.java))
        stubResponse(receiver, message, "N", errorResponse)

        // when
        val thrown = catchThrowable { client.sendSms(receiver, message, false) }

        // then
        assertThat(thrown).isInstanceOf(ExternalApiException::class.java)
        val errorCode = (thrown as ExternalApiException).errorCode
        assertThat(errorCode).isEqualTo(ExternalErrors.SEND_SMS_API_ERROR)
        assertThat(errorCode.httpStatus).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
        assertThat(errorCode.message).isEqualTo("Failed to send SMS due to an error in the external SMS service.")
        assertThat(logAppender.list).hasSize(1)
        assertThat(logAppender.list.single().level).isEqualTo(Level.ERROR)
        assertThat(logAppender.list.single().formattedMessage).contains(errorResponse.message)
    }

    private fun stubResponse(receiver: String, message: String, testMode: String, response: SendSmsResponse) {
        `when`(
            aligoHttpClient.sendSms(
                properties.apiKey, properties.userId, properties.sender, receiver, message, testMode, "SMS",
            )
        ).thenReturn(response)
    }

    private fun response(
        resultCode: Int,
        message: String = Instancio.create(String::class.java),
    ): SendSmsResponse = SendSmsResponse(
        resultCode = resultCode,
        message = message,
        messageId = 1,
        successCnt = if (resultCode >= 0) 1 else 0,
        errorCount = if (resultCode >= 0) 0 else 1,
        messageType = "SMS",
    )

}
