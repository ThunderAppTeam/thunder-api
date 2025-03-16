package app.thunder.api.domain.review.adapter

import app.thunder.api.controller.response.GetReviewableResponse
import app.thunder.api.domain.review.entity.DummyDeckEntity
import app.thunder.api.domain.review.repository.DummyDeckJdbcRepository
import app.thunder.api.domain.review.repository.DummyDeckRepository
import app.thunder.api.domain.review.repository.findAllByMemberIdOrderByCreatedAt
import app.thunder.domain.photo.BodyPhoto
import app.thunder.domain.photo.BodyPhotoAdapter
import app.thunder.storage.db.member.MemberRepository
import java.util.Locale
import javax.annotation.PostConstruct
import kotlin.random.Random
import net.datafaker.Faker
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class DummyDeckAdapter(
    private val dummyDeckRepository: DummyDeckRepository,
    private val bodyPhotoAdapter: BodyPhotoAdapter,
    private val dummyDeckJdbcRepository: DummyDeckJdbcRepository,
    private val memberRepository: MemberRepository,
) {

    private val faker = Faker(Locale.Builder().setLanguage("ko").setRegion("KR").build())
    private var maleMemberId: Long = 0L
    private var femaleMemberId: Long = 0L
    private var dummyBodyPhotoMap: Map<Long, BodyPhoto> = hashMapOf()

    @PostConstruct
    fun setUp() {
        maleMemberId = memberRepository.findByNickname("dummy1")?.memberId ?: 0
        femaleMemberId = memberRepository.findByNickname("dummy2")?.memberId ?: 0

        val bodyPhotos = bodyPhotoAdapter.getAllByMemberId(maleMemberId) +
                bodyPhotoAdapter.getAllByMemberId(femaleMemberId)
        dummyBodyPhotoMap = bodyPhotos.associateBy { it.bodyPhotoId }
    }

    @Transactional
    fun getAllByMemberId(memberId: Long): List<GetReviewableResponse> {
        var dummyDeckEntities = dummyDeckRepository.findAllByMemberIdOrderByCreatedAt(memberId)
        if (dummyDeckEntities.isEmpty()) {
            val nicknameToAgeMap = this.generateNicknameToAgeMap(dummyBodyPhotoMap.size)
            val nicknameList = nicknameToAgeMap.keys.toList()
            dummyDeckEntities = dummyBodyPhotoMap.values
                .shuffled()
                .mapIndexed { index, bodyPhoto ->
                    val nickname = nicknameList[index]
                    val age = nicknameToAgeMap[nickname] ?: Random.nextInt(20, 38)
                    DummyDeckEntity.create(memberId = memberId,
                                           bodyPhotoId = bodyPhoto.bodyPhotoId,
                                           bodyPhotoMemberId = bodyPhoto.memberId,
                                           nickname = nickname,
                                           age = age)
                }
            dummyDeckJdbcRepository.batchInsert(dummyDeckEntities)
        }

        return dummyDeckEntities.mapNotNull {
            val bodyPhoto = dummyBodyPhotoMap[it.bodyPhotoId] ?: return@mapNotNull null
            GetReviewableResponse(
                it.bodyPhotoId,
                bodyPhoto.imageUrl,
                bodyPhoto.memberId,
                it.nickname,
                it.age,
            )
        }
    }

    private fun generateNicknameToAgeMap(length: Int): HashMap<String, Int> {
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