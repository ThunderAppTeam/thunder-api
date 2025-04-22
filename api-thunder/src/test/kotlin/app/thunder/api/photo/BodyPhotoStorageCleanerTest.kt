package app.thunder.api.photo

import app.thunder.api.application.BodyPhotoStorageCleaner
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate

class BodyPhotoStorageCleanerTest : BodyPhotoWebTest() {

    @Autowired
    private lateinit var cleaner: BodyPhotoStorageCleaner

    @Autowired
    private lateinit var transactionManager: PlatformTransactionManager

    @Test
    @DisplayName("커밋 후 삭제를 등록하면 커밋 전에는 지우지 않고 커밋 뒤에 지운다")
    fun deleteAfterCommit() {
        // given
        val transaction = TransactionTemplate(transactionManager)

        // when
        transaction.executeWithoutResult {
            cleaner.deleteAfterCommit(listOf(URL_1, URL_2))
            verify(bodyPhotoStorageClient, never()).delete(anyString())
        }

        // then
        verify(bodyPhotoStorageClient).delete(URL_1)
        verify(bodyPhotoStorageClient).delete(URL_2)
    }

    @Test
    @DisplayName("커밋 후 삭제를 등록한 트랜잭션이 롤백되면 지우지 않는다")
    fun deleteAfterCommitSkippedOnRollback() {
        // given
        val transaction = TransactionTemplate(transactionManager)

        // when
        transaction.executeWithoutResult {
            cleaner.deleteAfterCommit(listOf(URL_1))
            it.setRollbackOnly()
        }

        // then
        verify(bodyPhotoStorageClient, never()).delete(anyString())
    }

    @Test
    @DisplayName("롤백 시 삭제를 등록한 트랜잭션이 롤백되면 그 객체를 지운다")
    fun deleteOnRollback() {
        // given
        val transaction = TransactionTemplate(transactionManager)

        // when
        transaction.executeWithoutResult {
            cleaner.deleteOnRollback(URL_1)
            verify(bodyPhotoStorageClient, never()).delete(anyString())
            it.setRollbackOnly()
        }

        // then
        verify(bodyPhotoStorageClient).delete(URL_1)
    }

    @Test
    @DisplayName("롤백 시 삭제를 등록한 트랜잭션이 커밋되면 지우지 않는다")
    fun deleteOnRollbackSkippedOnCommit() {
        // given
        val transaction = TransactionTemplate(transactionManager)

        // when
        transaction.executeWithoutResult { cleaner.deleteOnRollback(URL_1) }

        // then
        verify(bodyPhotoStorageClient, never()).delete(anyString())
    }

    @Test
    @DisplayName("저장소 삭제가 실패해도 커밋은 예외 없이 끝나고 나머지 객체를 계속 지운다")
    fun storageFailureDoesNotPropagate() {
        // given
        doThrow(IllegalStateException("s3 down")).`when`(bodyPhotoStorageClient).delete(URL_1)
        val transaction = TransactionTemplate(transactionManager)

        // when
        transaction.executeWithoutResult { cleaner.deleteAfterCommit(listOf(URL_1, URL_2)) }

        // then
        verify(bodyPhotoStorageClient).delete(URL_2)
    }

    companion object {
        private const val URL_1 = "https://test-bucket.s3.ap-northeast-2.amazonaws.com/1/body_photo/a.jpg"
        private const val URL_2 = "https://test-bucket.s3.ap-northeast-2.amazonaws.com/1/body_photo/b.jpg"
    }

}
