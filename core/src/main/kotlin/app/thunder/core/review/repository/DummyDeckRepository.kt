package app.thunder.core.review.repository

import app.thunder.core.review.domain.DummyDeck

interface DummyDeckRepository {
    fun getAllByMemberId(memberId: Long): List<DummyDeck>
    fun deleteByMemberIdAndBodyPhotoId(memberId: Long, bodyPhotoId: Long): Boolean
}
