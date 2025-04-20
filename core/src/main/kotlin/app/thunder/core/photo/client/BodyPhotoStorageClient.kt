package app.thunder.core.photo.client

interface BodyPhotoStorageClient {
    fun upload(file: ByteArray, contentType: String?, filePath: String): String

    fun delete(imageUrl: String)
}
