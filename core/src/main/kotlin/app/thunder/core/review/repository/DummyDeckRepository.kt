package app.thunder.core.review.repository

import app.thunder.core.review.domain.DummyDeck

interface DummyDeckRepository {
    fun getAllByMemberId(memberId: Long): List<DummyDeck>
    fun exists(memberId: Long, bodyPhotoId: Long): Boolean
    fun deleteByMemberIdAndBodyPhotoId(memberId: Long, bodyPhotoId: Long): Boolean
    fun deleteAllByMemberId(memberId: Long)
}
