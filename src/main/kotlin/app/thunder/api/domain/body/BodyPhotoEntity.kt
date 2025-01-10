package app.thunder.api.domain.body

import jakarta.persistence.*
import java.time.LocalDateTime

@Table(name = "body_photo")
@Entity
class BodyPhotoEntity private constructor(
    memberId: Long,
    imageUrl: String,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "body_photo_id")
    val bodyPhotoId: Long = 0

    @Column(name = "member_id")
    val memberId: Long = memberId

    @Column(name = "image_url")
    val imageUrl: String = imageUrl

    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now()


    companion object {
        fun create(
            memberId: Long,
            imageUrl: String,
        ): BodyPhotoEntity {
            return BodyPhotoEntity(
                memberId = memberId,
                imageUrl = imageUrl,
            )
        }
    }

}