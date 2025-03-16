package app.thunder.domain.member

enum class MemberDeletionReason(
    val descriptionKR: String,
) {
    DECREASED_INTEREST("서비스에 대한 관심이 줄어듬"),
    PRIVACY_CONCERNS("내 사진을 타인에게 노출하는 것이 부담스러움"),
    UNRELIABLE_BODY_CHECK_RESULTS("눈바디 측정 결과를 신뢰할 수 없음"),
    LONG_BODY_CHECK_PROCESS("눈바디 측정 과정이 너무 오래 걸림"),
    TOO_MANY_ERRORS("서비스 오류가 잦음"),
    INCONVENIENT_SERVICE("서비스 이용이 불편함"),
    FOUND_BETTER_ALTERNATIVE("더 나은 서비스를 발견함"),
    OTHER("기타"),
}