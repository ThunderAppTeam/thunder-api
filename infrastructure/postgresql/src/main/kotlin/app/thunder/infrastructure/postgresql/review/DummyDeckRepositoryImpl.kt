package app.thunder.infrastructure.postgresql.review

import app.thunder.core.photo.domain.BodyPhoto
import app.thunder.core.review.domain.DummyDeck
import app.thunder.core.review.repository.DummyDeckRepository
import app.thunder.infrastructure.postgresql.member.persistence.MemberJpaRepository
import app.thunder.infrastructure.postgresql.photo.BodyPhotoJpaRepository
import app.thunder.infrastructure.postgresql.photo.toDomain
import app.thunder.infrastructure.postgresql.review.entity.DummyDeckEntity
import app.thunder.infrastructure.postgresql.review.jdbc.DummyDeckJdbcRepository
import app.thunder.infrastructure.postgresql.review.persistence.DummyDeckJpaRepository
import jakarta.annotation.PostConstruct
import java.util.Locale
import kotlin.random.Random
import net.datafaker.Faker
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
internal class DummyDeckRepositoryImpl(
    private val dummyDeckJpaRepository: DummyDeckJpaRepository,
    private val bodyPhotoJpaRepository: BodyPhotoJpaRepository,
    private val dummyDeckJdbcRepository: DummyDeckJdbcRepository,
    private val memberJpaRepository: MemberJpaRepository,
) : DummyDeckRepository {

    private val faker = Faker(Locale.Builder().setLanguage("ko").setRegion("KR").build())
    private var maleMemberId: Long = 0L
    private var femaleMemberId: Long = 0L
    private var dummyBodyPhotoMap: Map<Long, BodyPhoto> = hashMapOf()

    @PostConstruct
    fun setUp() {
        maleMemberId = memberJpaRepository.findByNickname("dummy1")?.memberId ?: 0
        femaleMemberId = memberJpaRepository.findByNickname("dummy2")?.memberId ?: 0

        val bodyPhotos = (bodyPhotoJpaRepository.findAllByMemberId(maleMemberId) +
                bodyPhotoJpaRepository.findAllByMemberId(femaleMemberId))
            .map { it.toDomain() }
        dummyBodyPhotoMap = bodyPhotos.associateBy { it.bodyPhotoId }
    }

    @Transactional
    override fun getAllByMemberId(memberId: Long): List<DummyDeck> {
        var dummyDeckEntities = dummyDeckJpaRepository.findAllByMemberIdOrderByCreatedAt(memberId)
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

        return dummyDeckEntities.mapNotNull { dummyDeckEntity ->
            val dummyPhoto = dummyBodyPhotoMap[dummyDeckEntity.bodyPhotoId] ?: return@mapNotNull null
            DummyDeck(
                dummyDeckEntity.dummyDeckId,
                dummyDeckEntity.bodyPhotoId,
                dummyDeckEntity.bodyPhotoMemberId,
                dummyPhoto.imageUrl,
                dummyDeckEntity.nickname,
                dummyDeckEntity.age,
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
    override fun deleteByMemberIdAndBodyPhotoId(memberId: Long, bodyPhotoId: Long): Boolean {
        val dummyDeckEntity = dummyDeckJpaRepository.findByMemberIdAndBodyPhotoId(memberId, bodyPhotoId)
            ?: return false
        dummyDeckJpaRepository.deleteById(dummyDeckEntity.dummyDeckId)
        return true
    }

}
