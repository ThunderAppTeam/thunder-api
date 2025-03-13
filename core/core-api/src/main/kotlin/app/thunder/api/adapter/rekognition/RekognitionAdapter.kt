package app.thunder.api.adapter.rekognition

import app.thunder.api.adapter.rekognition.BodyRekognition.RekognitionLabel
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.rekognition.RekognitionClient
import software.amazon.awssdk.services.rekognition.model.DetectLabelsRequest
import software.amazon.awssdk.services.rekognition.model.Image

@Component
class RekognitionAdapter(
    private val rekognitionClient: RekognitionClient,
) {

    companion object {
        private val PERSON_LABELS = setOf(
            "Person", "Male", "Female", "Man", "Woman", "Shoulder", "Leg Press", "Arm", "Knee", "Neck", "Bicep Curls",
            "Wrist", "Back", "Ankle", "Finger", "Hip", "Toe", "Calf", "Thigh", "Body Part"
        )
    }

    fun getBodyRekognition(imageFile: MultipartFile): BodyRekognition {
        val image = Image.builder()
            .bytes(SdkBytes.fromByteArray(imageFile.bytes))
            .build()
        val request = DetectLabelsRequest.builder()
            .image(image)
            .minConfidence(90f)
            .maxLabels(100)
            .build()

        val response = rekognitionClient.detectLabels(request)

        var isDetectedBody = false
        val labels = response.labels().map { label ->
            if (PERSON_LABELS.contains(label.name())) {
                isDetectedBody = true
            }
            RekognitionLabel(label.name(), label.confidence())
        }

        return BodyRekognition(isDetectedBody, labels)
    }

}