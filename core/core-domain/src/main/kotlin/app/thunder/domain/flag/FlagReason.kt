package app.thunder.domain.flag

enum class FlagReason(
    val descriptionKR: String,
) {
    SEXUAL_CONTENT("성적인 콘텐츠"),
    IRRELEVANT_BODY_CHECK("눈바디와 관련 없는 콘텐츠"),
    IMPERSONATION("타인 사칭 또는 사진 도용"),
    VERBAL_ABUSE("비속어 또는 혐오 표현"),
    CHILD_SEXUAL_ABUSE("아동 및 청소년 성착취물"),
    OTHER("기타"),
}
