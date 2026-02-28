package app.thunder.domain.photo

interface BodyPhotoStoragePort {
    fun upload(file: ByteArray, contentType: String?, filePath: String): String

    fun delete(imageUrl: String)
}
