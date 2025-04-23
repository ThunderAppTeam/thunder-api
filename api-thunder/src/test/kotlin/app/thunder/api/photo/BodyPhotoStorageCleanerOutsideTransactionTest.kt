package app.thunder.api.photo

import app.thunder.api.application.BodyPhotoStorageCleaner
import app.thunder.core.photo.client.BodyPhotoStorageClient
import org.assertj.core.api.Assertions.assertThat
import org.instancio.Instancio
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions

class BodyPhotoStorageCleanerOutsideTransactionTest {

    private val storageClient = mock(BodyPhotoStorageClient::class.java)
    private val cleaner = BodyPhotoStorageCleaner(storageClient)

    @Test
    @DisplayName("트랜잭션 밖에서 커밋 후 삭제를 등록하면 IllegalStateException이 발생하고 아무것도 지우지 않는다")
    fun deleteAfterCommitOutsideTransaction() {
        // given
        val imageUrls = listOf(Instancio.create(String::class.java))

        // when
        val thrown = runCatching { cleaner.deleteAfterCommit(imageUrls) }.exceptionOrNull()

        // then
        assertThat(thrown).isInstanceOf(IllegalStateException::class.java)
            .hasMessage("Storage cleanup must be registered inside a transaction.")
        verifyNoInteractions(storageClient)
    }

    @Test
    @DisplayName("트랜잭션 밖에서 롤백 시 삭제를 등록하면 IllegalStateException이 발생하고 아무것도 지우지 않는다")
    fun deleteOnRollbackOutsideTransaction() {
        // given
        val imageUrl = Instancio.create(String::class.java)

        // when
        val thrown = runCatching { cleaner.deleteOnRollback(imageUrl) }.exceptionOrNull()

        // then
        assertThat(thrown).isInstanceOf(IllegalStateException::class.java)
            .hasMessage("Storage cleanup must be registered inside a transaction.")
        verifyNoInteractions(storageClient)
    }

}
