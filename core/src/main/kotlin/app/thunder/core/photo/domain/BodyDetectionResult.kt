package app.thunder.core.photo.domain

data class BodyDetectionResult(
    val isDetectedBody: Boolean,
    val details: String,
)
