# 구조와 의존 규칙

> 이 문서는 thunder-api의 모듈 배치, 의존 방향, 이름 규칙을 정한다.<br>
> 여기 적힌 규칙 대부분은 `testing:architecture` 테스트가 빌드할 때 검사한다.

## 모듈 구성

| 모듈 | 맡는 일 | 패키지 |
|---|---|---|
| `api-thunder` | 실행 앱. HTTP 요청을 받고 업무 흐름을 조립한다 | `app.thunder.api` |
| `core` | 도메인 객체와 규칙, DB 저장과 외부 호출 계약 | `app.thunder.core.<domain>.<kind>` |
| `infrastructure:postgresql` | DB 계약 구현. JPA, JDBC, Flyway migration | `app.thunder.infrastructure.postgresql` |
| `infrastructure:aws` | S3 사진 저장, Rekognition 신체 감지 | `app.thunder.infrastructure.aws` |
| `infrastructure:firebase` | FCM 푸시 알림 | `app.thunder.infrastructure.firebase` |
| `infrastructure:client` | Aligo SMS 발송 | `app.thunder.infrastructure.client` |
| `libraries:errors` | 오류 코드와 예외 타입 | `app.thunder.libraries.errors` |
| `testing:architecture` | 구조 규칙 테스트 | `app.thunder.testing.architecture` |

`build-logic`은 모든 모듈이 쓰는 Gradle convention plugin을 담는다.<br>
`thunder.kotlin`은 Kotlin 설정, `thunder.library`는 일반 jar, `thunder.spring`은 Spring 설정,
`thunder.runtime`은 실행 jar(bootJar)를 맡는다.<br>
의존성 버전은 `gradle/libs.versions.toml` 한 곳에서 관리한다.

## 의존 방향

Gradle 모듈 의존은 아래 표에 있는 대상만 허용한다.

| 출발 모듈 | 허용 대상 |
|---|---|
| `core` | 없음 |
| `infrastructure:*` | `core`, `libraries:*` |
| `api-thunder` | `core`, `libraries:*`, `infrastructure:*` |
| `libraries:*` | 다른 `libraries:*` |
| `testing:*` | 없음. 소스 파일을 직접 읽어 검사한다 |

코드 import에는 아래 규칙을 더한다.

- `core`는 Spring, `jakarta`, infrastructure, api, libraries 패키지를 import하지 않는다.
- `api-thunder`는 infrastructure에서 `*InfrastructureConfiguration`만 import한다.<br>
  서비스는 core 계약(`*Repository`, `*Client`)만 주입받고, 구현체는 Spring이 연결한다.
- infrastructure 모듈끼리는 서로 import하지 않는다.
- 다른 core 도메인의 타입은 그 도메인의 `domain` 패키지에서만 가져온다.

## core 패키지

core 파일은 `app.thunder.core.<domain>.<kind>`에 둔다.<br>
도메인은 `admin`, `flag`, `member`, `notification`, `photo`, `review`다.

| kind | 담는 것 | 예 |
|---|---|---|
| `domain` | 도메인 객체, 값, enum | `Member`, `Gender` |
| `repository` | DB 저장·조회 계약 interface | `MemberRepository` |
| `client` | 외부 시스템 호출 계약 interface | `SmsClient` |
| `command` | 계약 메서드의 입력 묶음 | `CreateMemberCommand` |

## 이름 규칙

| 구분 | core 계약 | infrastructure 구현체 | 판단 기준 |
|---|---|---|---|
| DB 저장·조회 | `*Repository` | `internal class *RepositoryImpl` | 도메인 객체를 저장했다가 다시 읽는다 |
| 외부 시스템 호출 | `*Client` | `internal class *ClientImpl` | 외부 API에 요청을 보내고 끝난다. S3 파일 저장도 여기에 속한다 |
| Spring Data 인터페이스 | 없음 | `internal interface *JpaRepository` | 구현체 안에서만 쓴다 |
| 외부 API 호출 부품 | 없음 | `AligoHttpClient` 같은 기술 이름 | 구현체 안에서만 쓴다 |

- `Port`, `Adapter`로 끝나는 이름은 쓰지 않는다.
- 구현체 파일 하나는 core 계약 하나만 구현하고, 다른 core 계약을 주입받지 않는다.<br>
  여러 계약을 묶는 일은 `api-thunder`의 서비스가 맡는다.
- Spring Data 인터페이스 이름은 `*JpaRepository`로 유지한다.<br>
  그래야 core 계약과 같은 이름이 두 곳에 생기지 않는다.<br>
  Spring Data는 `<JPA 인터페이스 이름>Impl` 클래스를 custom 구현으로 연결하므로 `*RepositoryImpl`과 겹치지 않는다.

## 규칙 검사

`./gradlew build`가 `testing:architecture` 테스트를 함께 실행하고, 위반이 있으면 빌드가 실패한다.

| 테스트 클래스 | 검사하는 규칙 |
|---|---|
| `ModuleStructureRulesTest` | 모듈 경로, 모듈별 패키지, Gradle 모듈 의존 표 |
| `CoreRulesTest` | core의 금지 import, 계약 이름, 도메인 간 import |
| `ImplementationRulesTest` | `Port`/`Adapter` 이름 금지, 구현체당 계약 하나와 `internal class *Impl` |
| `BoundaryRulesTest` | infrastructure 모듈 간 import 금지, api의 infrastructure import 범위, libraries 독립 |

규칙을 바꿀 때는 이 문서와 테스트를 함께 바꾼다.

## 보류한 구조

아래 항목은 참고한 Ais 저장소에 있지만, 지금 규모에서는 얻는 것보다 관리 비용이 커서 두지 않는다.

| 항목 | 지금 구조 | 다시 검토할 때 |
|---|---|---|
| Reader/Writer 계층 | 서비스가 core 계약을 바로 호출한다 | 여러 서비스가 같은 조회·쓰기 조합을 반복할 때 |
| 도메인별 core 모듈 | `core` 모듈 하나에서 도메인을 패키지로 나눈다 | 실행 앱이 늘어 모듈마다 필요한 도메인이 달라질 때 |
| 도메인별 postgresql 모듈 | `infrastructure:postgresql` 하나 | 위와 같다 |
