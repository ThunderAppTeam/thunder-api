package app.thunder.infrastructure.aws.rekognition

import org.assertj.core.api.Assertions.assertThat
import org.instancio.Instancio
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import software.amazon.awssdk.services.rekognition.RekognitionClient
import software.amazon.awssdk.services.rekognition.model.DetectLabelsRequest
import software.amazon.awssdk.services.rekognition.model.DetectLabelsResponse
import software.amazon.awssdk.services.rekognition.model.Label

class BodyDetectionClientImplTest {

    private lateinit var rekognitionClient: RekognitionClient
    private lateinit var client: BodyDetectionClientImpl

    @BeforeEach
    fun setUp() {
        rekognitionClient = mock(RekognitionClient::class.java)
        client = BodyDetectionClientImpl(rekognitionClient)
    }

    @Test
    @DisplayName("이미지 바이트를 신뢰도 90, 최대 라벨 100개 조건으로 Rekognition에 보낸다")
    fun sendsImageWithDetectionConditions() {
        // given
        val imageFile = Instancio.create(ByteArray::class.java)
        stubLabels()

        // when
        client.detectBody(imageFile)

        // then
        val request = capturedRequest()
        assertThat(request.image().bytes().asByteArray()).isEqualTo(imageFile)
        assertThat(request.minConfidence()).isEqualTo(90f)
        assertThat(request.maxLabels()).isEqualTo(100)
    }

    @ParameterizedTest
    @ValueSource(strings = ["Person", "Male", "Female", "Man", "Woman", "Body Part", "Thigh", "Bicep Curls"])
    @DisplayName("사람 관련 라벨이 하나라도 있으면 몸이 감지된 것으로 판단하고 모든 라벨을 상세에 담는다")
    fun detectsBodyWhenPersonLabelExists(personLabel: String) {
        // given
        val otherLabel = Instancio.create(String::class.java)
        stubLabels(label(otherLabel, 95.5f), label(personLabel, 99.5f))

        // when
        val result = client.detectBody(Instancio.create(ByteArray::class.java))

        // then
        assertThat(result.isDetectedBody).isTrue()
        assertThat(result.details)
            .isEqualTo("[$otherLabel]: 95.5${System.lineSeparator()}[$personLabel]: 99.5")
    }

    @Test
    @DisplayName("사람 관련 라벨이 없으면 몸이 감지되지 않은 것으로 판단한다")
    fun doesNotDetectBodyWithoutPersonLabel() {
        // given
        val firstLabel = Instancio.create(String::class.java)
        val secondLabel = Instancio.create(String::class.java)
        stubLabels(label(firstLabel, 97f), label(secondLabel, 92.5f))

        // when
        val result = client.detectBody(Instancio.create(ByteArray::class.java))

        // then
        assertThat(result.isDetectedBody).isFalse()
        assertThat(result.details).isEqualTo("[$firstLabel]: 97.0${System.lineSeparator()}[$secondLabel]: 92.5")
    }

    @Test
    @DisplayName("Rekognition이 라벨을 하나도 돌려주지 않으면 감지되지 않고 상세는 비어 있다")
    fun detectsNothingWithoutLabels() {
        // given
        stubLabels()

        // when
        val result = client.detectBody(Instancio.create(ByteArray::class.java))

        // then
        assertThat(result.isDetectedBody).isFalse()
        assertThat(result.details).isEmpty()
    }

    private fun stubLabels(vararg labels: Label) {
        val response = DetectLabelsResponse.builder().labels(labels.toList()).build()
        `when`(rekognitionClient.detectLabels(any(DetectLabelsRequest::class.java))).thenReturn(response)
    }

    private fun label(name: String, confidence: Float): Label =
        Label.builder().name(name).confidence(confidence).build()

    private fun capturedRequest(): DetectLabelsRequest {
        val captor = ArgumentCaptor.forClass(DetectLabelsRequest::class.java)
        verify(rekognitionClient).detectLabels(captor.capture())
        return captor.value
    }

}
