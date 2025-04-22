package app.thunder.api.application

import app.thunder.core.photo.client.BodyPhotoStorageClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager

@Component
class BodyPhotoStorageCleaner(
    private val bodyPhotoStorageClient: BodyPhotoStorageClient,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun deleteAfterCommit(imageUrls: Collection<String>) {
        register(object : TransactionSynchronization {
            override fun afterCommit() {
                imageUrls.forEach(::deleteQuietly)
            }
        })
    }

    fun deleteOnRollback(imageUrl: String) {
        register(object : TransactionSynchronization {
            override fun afterCompletion(status: Int) {
                if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
                    deleteQuietly(imageUrl)
                }
            }
        })
    }

    private fun register(synchronization: TransactionSynchronization) {
        check(TransactionSynchronizationManager.isSynchronizationActive()) {
            "Storage cleanup must be registered inside a transaction."
        }
        TransactionSynchronizationManager.registerSynchronization(synchronization)
    }

    private fun deleteQuietly(imageUrl: String) {
        try {
            bodyPhotoStorageClient.delete(imageUrl)
        } catch (e: Exception) {
            log.error("Body photo storage delete failed [imageUrl: {}]", imageUrl, e)
        }
    }

}
