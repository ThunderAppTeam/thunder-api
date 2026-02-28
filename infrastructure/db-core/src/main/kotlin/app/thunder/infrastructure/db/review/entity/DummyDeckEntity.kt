package app.thunder.infrastructure.db.review.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Table(name = "dummy_deck")
@Entity
internal class DummyDeckEntity private constructor(
    memberId: Long,
    bodyPhotoId: Long,
    bodyPhotoMemberId: Long,
    nickname: String,
    age: Int,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "dummy_deck_id", nullable = false)
    val dummyDeckId: Long = 0

    @Column(name = "member_id", nullable = false)
    val memberId: Long = memberId

    @Column(name = "body_photo_id", nullable = false)
    val bodyPhotoId: Long = bodyPhotoId

    @Column(name = "body_photo_member_id", nullable = false)
    val bodyPhotoMemberId: Long = bodyPhotoMemberId

    @Column(name = "nickname", nullable = false)
    val nickname: String = nickname

    @Column(name = "age", nullable = false)
    val age: Int = age

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()


    companion object {
        internal fun create(
            memberId: Long,
            bodyPhotoId: Long,
            bodyPhotoMemberId: Long,
            nickname: String,
            age: Int,
        ): DummyDeckEntity {

            return DummyDeckEntity(
                memberId = memberId,
                bodyPhotoId = bodyPhotoId,
                bodyPhotoMemberId = bodyPhotoMemberId,
                nickname = nickname,
                age = age,
            )
        }
    }

}