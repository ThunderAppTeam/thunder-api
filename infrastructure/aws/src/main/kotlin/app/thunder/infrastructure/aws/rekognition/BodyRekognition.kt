package app.thunder.infrastructure.aws.rekognition

data class BodyRekognition(
    val isDetectedBody: Boolean,
    val labelList: List<RekognitionLabel>
) {

    data class RekognitionLabel(
        val name: String,
        val confidence: Float,
    )

    override fun toString(): String {
        return labelList.joinToString(separator = System.lineSeparator()) {
            "[${it.name}]: ${it.confidence}"
        }
    }

}
