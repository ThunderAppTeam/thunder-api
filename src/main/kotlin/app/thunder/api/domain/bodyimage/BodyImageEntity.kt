package app.thunder.api.domain.bodyimage

import jakarta.persistence.*
import java.time.LocalDateTime

@Table(name = "body_image")
//@Entity
class BodyImageEntity private constructor(
    memberId: Long,
    imageUrl: String,
    createdBy: Long,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "body_image_id")
    val bodyImageId: Long = 0

    @Column(name = "member_id")
    val memberId: Long = memberId

    @Column(name = "s3_image_url")
    var imageUrl: String = imageUrl
        protected set

    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now()

    @Column(name = "created_at")
    val createdBy: Long = createdBy

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null
        protected set

    @Column(name = "updated_by")
    var updatedBy: Long? = null
        protected set


    companion object {
        fun create(
            memberId: Long,
            imageUrl: String,
            createdBy: Long,
        ): BodyImageEntity {
            return BodyImageEntity(
                memberId = memberId,
                imageUrl = imageUrl,
                createdBy = createdBy
            )
        }
    }

}