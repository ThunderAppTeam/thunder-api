package app.thunder.core.photo.client

import app.thunder.core.photo.domain.BodyDetectionResult

interface BodyDetectionClient {
    fun detectBody(imageFile: ByteArray): BodyDetectionResult
}
