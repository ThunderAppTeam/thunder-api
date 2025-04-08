package app.thunder.domain.review

interface DummyDeckPort {
    fun getAllByMemberId(memberId: Long): List<DummyDeck>
    fun deleteByMemberIdAndBodyPhotoId(memberId: Long, bodyPhotoId: Long): Boolean
}
