package app.thunder.domain.photo

interface BodyDetectionPort {
    fun detectBody(imageFile: ByteArray): BodyDetectionResult
}
