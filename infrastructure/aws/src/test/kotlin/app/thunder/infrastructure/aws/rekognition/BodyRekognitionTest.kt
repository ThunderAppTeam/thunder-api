package app.thunder.infrastructure.aws.rekognition

import app.thunder.infrastructure.aws.rekognition.BodyRekognition.RekognitionLabel
import org.assertj.core.api.Assertions.assertThat
import org.instancio.Instancio
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class BodyRekognitionTest {

    @Test
    @DisplayName("라벨마다 이름과 신뢰도를 한 줄씩 줄바꿈으로 이어 문자열로 만든다")
    fun toStringJoinsLabelsByLine() {
        // given
        val first = Instancio.create(String::class.java)
        val second = Instancio.create(String::class.java)
        val rekognition = BodyRekognition(
            isDetectedBody = true,
            labelList = listOf(RekognitionLabel(first, 99.5f), RekognitionLabel(second, 91.25f)),
        )

        // when
        val text = rekognition.toString()

        // then
        assertThat(text).isEqualTo("[$first]: 99.5${System.lineSeparator()}[$second]: 91.25")
    }

    @Test
    @DisplayName("라벨이 없으면 빈 문자열을 돌려준다")
    fun toStringIsEmptyWithoutLabels() {
        // given
        val rekognition = BodyRekognition(isDetectedBody = false, labelList = emptyList())

        // when
        val text = rekognition.toString()

        // then
        assertThat(text).isEmpty()
    }

}
