package app.thunder.infrastructure.postgresql.photo

import app.thunder.core.photo.domain.BodyPhoto

internal fun BodyPhotoEntity.toDomain(): BodyPhoto {
    return BodyPhoto(
        bodyPhotoId,
        memberId,
        imageUrl,
        reviewCount,
        totalReviewScore,
        createdAt,
        updatedAt,
    )
}
