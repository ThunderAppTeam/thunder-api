package app.thunder.api.domain.review.adapter

import app.thunder.api.controller.response.GetReviewableResponse
import app.thunder.api.domain.member.repository.MemberRepository
import app.thunder.api.domain.photo.BodyPhotoEntity
import app.thunder.api.domain.photo.BodyPhotoRepository
import app.thunder.api.domain.photo.findAllByMemberId
import app.thunder.api.domain.review.entity.DummyDeckEntity
import app.thunder.api.domain.review.repository.DummyDeckJdbcRepository
import app.thunder.api.domain.review.repository.DummyDeckRepository
import app.thunder.api.domain.review.repository.findAllByMemberIdOrderByCreatedAt
import java.util.Locale
import javax.annotation.PostConstruct
import kotlin.random.Random
import net.datafaker.Faker
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class DummyDeckAdapter(
    private val dummyDeckRepository: DummyDeckRepository,
    private val bodyPhotoRepository: BodyPhotoRepository,
    private val dummyDeckJdbcRepository: DummyDeckJdbcRepository,
    private val memberRepository: MemberRepository,
) {

    private val faker = Faker(Locale.Builder().setLanguage("ko").setRegion("KR").build())
    private var maleMemberId: Long = 0L
    private var femaleMemberId: Long = 0L
    private var dummyBodyPhotoMap: Map<Long, BodyPhotoEntity> = hashMapOf()

    @PostConstruct
    fun setUp() {
        maleMemberId = memberRepository.findByNickname("dummy1")?.memberId ?: 0
        femaleMemberId = memberRepository.findByNickname("dummy2")?.memberId ?: 0

        val bodyPhotoEntities = bodyPhotoRepository.findAllByMemberId(maleMemberId) +
                bodyPhotoRepository.findAllByMemberId(femaleMemberId)
        dummyBodyPhotoMap = bodyPhotoEntities.associateBy { it.bodyPhotoId }
    }

    @Transactional
    fun getAllByMemberId(memberId: Long): List<GetReviewableResponse> {
        var dummyDeckEntities = dummyDeckRepository.findAllByMemberIdOrderByCreatedAt(memberId)
        if (dummyDeckEntities.isEmpty()) {
            val nicknameToAgeMap = this.generateNicknameToAgeMap(dummyBodyPhotoMap.size)
            dummyDeckEntities = dummyBodyPhotoMap.values.map { bodyPhotoEntity ->
                val nickname = nicknameToAgeMap.keys.random()
                val age = nicknameToAgeMap[nickname] ?: Random.nextInt(20, 38)
                DummyDeckEntity.create(memberId, bodyPhotoEntity.bodyPhotoId, bodyPhotoEntity.memberId, nickname, age)
            }
            dummyDeckJdbcRepository.batchInsert(dummyDeckEntities)
        }

        return dummyDeckEntities.mapNotNull {
            val bodyPhotoEntity = dummyBodyPhotoMap[it.bodyPhotoId] ?: return@mapNotNull null
            GetReviewableResponse(
                it.bodyPhotoId,
                bodyPhotoEntity.imageUrl,
                bodyPhotoEntity.memberId,
                it.nickname,
                it.age,
            )
        }
    }

    private fun generateNicknameToAgeMap(length: Int): Map<String, Int> {
        val regex = Regex("[a-zA-Z$:()'-.0-9\\s]+")
        val result = hashMapOf<String, Int>()

        while (result.size < length) {
            faker.collection(
                { faker.kpop().girlGroups() },
                { faker.kpop().iGroups() },
                { faker.kpop().iiGroups() },
                { faker.kpop().iiiGroups() },
                { faker.kpop().solo() },
            ).len(length).generate<List<String>?>()
                .asSequence()
                .map { it.replace(regex, "") }
                .filter { it.length in 2..5 }
                .map { if (it.length > 2) it.drop(2) + it.take(2) else it }
                .forEach { result[it] = Random.nextInt(20, 38) }
        }

        return result
    }

    @Transactional
    fun deleteByMemberIdAndBodyPhotoId(memberId: Long, bodyPhotoId: Long): Boolean {
        val dummyDeckEntity = dummyDeckRepository.findByMemberIdAndBodyPhotoId(memberId, bodyPhotoId)
            ?: return false
        dummyDeckRepository.deleteById(dummyDeckEntity.dummyDeckId)
        return true
    }

}