package app.thunder.infrastructure.db.member.entity

import app.thunder.domain.member.MemberSettingOptions
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes

@Table(name = "member_setting")
@Entity
internal class MemberSettingEntity private constructor(
    memberId: Long,
    settings: MemberSettingOptions,
) {
    @Id
    @Column(name = "member_id")
    val memberId: Long = memberId

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "settings", columnDefinition = "jsonb", nullable = false)
    var settings: MemberSettingOptions = settings
        protected set

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime? = null
        protected set


    companion object {
        fun create(memberId: Long, memberSettingOptions: MemberSettingOptions): MemberSettingEntity {
            return MemberSettingEntity(memberId, memberSettingOptions)
        }
    }

    fun update(settings: MemberSettingOptions, updatedAt: LocalDateTime?) {
        this.settings = settings
        this.updatedAt = updatedAt
    }

}
