package app.thunder.api.domain.review

import app.thunder.api.domain.review.entity.DummyDeckEntity
import java.time.LocalDateTime

class DummyDeck private constructor(
    val dummyDeckId: Long,
    val memberId: Long,
    val bodyPhotoId: Long,
    val bodyPhotoMemberId: Long,
    val createdAt: LocalDateTime,
) {

    companion object {
        fun from(entity: DummyDeckEntity): DummyDeck {
            return DummyDeck(
                entity.dummyDeckId,
                entity.memberId,
                entity.bodyPhotoId,
                entity.bodyPhotoMemberId,
                entity.createdAt,
            )
        }
    }

}