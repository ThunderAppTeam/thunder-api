package app.thunder.infrastructure.aws.storage

import app.thunder.infrastructure.aws.storage.AwsProperties.AwsCredentialsProperties
import app.thunder.infrastructure.aws.storage.AwsProperties.AwsRegionProperties
import app.thunder.infrastructure.aws.storage.AwsProperties.AwsS3Properties
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest

class BodyPhotoStorageClientImplTest {

    private lateinit var s3Client: S3Client
    private lateinit var client: BodyPhotoStorageClientImpl

    @BeforeEach
    fun setUp() {
        s3Client = mock(S3Client::class.java)
        val properties = AwsProperties(
            credentials = AwsCredentialsProperties("access", "secret"),
            s3 = AwsS3Properties(BUCKET),
            region = AwsRegionProperties(REGION),
        )
        client = BodyPhotoStorageClientImpl(s3Client, properties)
    }

    @Test
    @DisplayName("저장된 URL에서 버킷 주소를 뺀 객체 키로 S3 객체를 지운다")
    fun deleteUsesObjectKey() {
        // given
        val imageUrl = "$URL_PREFIX/1/body_photo/0f0f0f0f.jpg"

        // when
        client.delete(imageUrl)

        // then
        val request = capturedDeleteRequest()
        assertThat(request.bucket()).isEqualTo(BUCKET)
        assertThat(request.key()).isEqualTo("1/body_photo/0f0f0f0f.jpg")
    }

    @Test
    @DisplayName("공백과 한글이 들어 있는 예전 URL도 인코딩하지 않은 객체 키로 지운다")
    fun deleteLegacyUrlWithSpacesAndNonAscii() {
        // given
        val imageUrl = "$URL_PREFIX/user nick/body_photo/0f0f0f0f_내 사진 (1).jpg"

        // when
        client.delete(imageUrl)

        // then
        assertThat(capturedDeleteRequest().key()).isEqualTo("user nick/body_photo/0f0f0f0f_내 사진 (1).jpg")
    }

    @Test
    @DisplayName("버킷 주소로 시작하지 않는 URL은 지우지 않는다")
    fun deleteSkipsForeignUrl() {
        // given
        val imageUrl = "https://other-bucket.s3.$REGION.amazonaws.com/1/body_photo/a.jpg"

        // when
        client.delete(imageUrl)

        // then
        verify(s3Client, never()).deleteObject(ArgumentCaptor.forClass(DeleteObjectRequest::class.java).capture())
    }

    private fun capturedDeleteRequest(): DeleteObjectRequest {
        val captor = ArgumentCaptor.forClass(DeleteObjectRequest::class.java)
        verify(s3Client).deleteObject(captor.capture())
        return captor.value
    }

    companion object {
        private const val BUCKET = "test-bucket"
        private const val REGION = "ap-northeast-2"
        private const val URL_PREFIX = "https://$BUCKET.s3.$REGION.amazonaws.com"
    }

}
