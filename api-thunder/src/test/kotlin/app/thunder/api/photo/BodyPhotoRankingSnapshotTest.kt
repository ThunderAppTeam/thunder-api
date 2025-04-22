package app.thunder.api.photo

import app.thunder.core.member.domain.Gender
import app.thunder.core.photo.repository.BodyPhotoRepository
import java.time.LocalDateTime
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.springframework.http.HttpHeaders.AUTHORIZATION
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class BodyPhotoRankingSnapshotTest : BodyPhotoWebTest() {

    @MockitoSpyBean
    private lateinit var bodyPhotoRepository: BodyPhotoRepository

    @BeforeEach
    fun createMembers() {
        insertMemberWithGender(OWNER_ID, "FEMALE")
        insertMemberWithGender(FEMALE_PEER_ID, "FEMALE")
    }

    @Test
    @DisplayName("순위 계산 중에 다른 사진의 리뷰가 완료되어도 상위 비율은 100%를 넘지 않는다")
    fun rateNeverExceedsHundredWhenPeerCompletesDuringRanking() {
        // given
        val target = insertBodyPhoto(OWNER_ID, reviewCount = 10, totalReviewScore = 40.0)
        val peer = insertBodyPhoto(FEMALE_PEER_ID, reviewCount = 9, totalReviewScore = 45.0)
        completePeerAfterPoolCount(peer)

        // when
        val result = mockMvc.perform(
            get("/v1/body/photo/$target").header(AUTHORIZATION, bearerToken(OWNER_ID)),
        )

        // then
        result.andExpect(status().isOk)
            .andExpect(jsonPath("$.data.genderTopRate").value(100.0))
    }

    private fun completePeerAfterPoolCount(peerPhotoId: Long) {
        Mockito.doAnswer { invocation ->
            val poolCount = invocation.callRealMethod()
            commitPeerCompletionOnOtherConnection(peerPhotoId)
            poolCount
        }.`when`(bodyPhotoRepository).countReviewCompletedByGender(anyGender(), anyLocalDateTime())
    }

    private fun commitPeerCompletionOnOtherConnection(peerPhotoId: Long) {
        val executor = Executors.newSingleThreadExecutor()
        try {
            executor.submit {
                jdbcTemplate.update(
                    "UPDATE body_photo SET review_count = 10, total_review_score = 50.0 WHERE body_photo_id = ?",
                    peerPhotoId,
                )
            }.get(COMMIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        } finally {
            executor.shutdownNow()
        }
    }

    private fun anyGender(): Gender {
        ArgumentMatchers.any(Gender::class.java)
        return Gender.FEMALE
    }

    private fun anyLocalDateTime(): LocalDateTime {
        ArgumentMatchers.any(LocalDateTime::class.java)
        return LocalDateTime.MIN
    }

    companion object {
        private const val OWNER_ID = 900_021L
        private const val FEMALE_PEER_ID = 900_022L
        private const val COMMIT_TIMEOUT_SECONDS = 10L
    }

}
