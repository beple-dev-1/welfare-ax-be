# 페이즈 1: 공통 인프라 준비

| 항목 | 내용 |
|------|------|
| 페이즈 번호 | 1 |
| 목표 | EXTERNAL_API_ERROR ErrorCode 추가 및 RestClient 의존성 확인 |
| 의존 페이즈 | 없음 |

---

## 구현 대상 파일

| 파일 경로 | 작업 유형 | 설명 |
|----------|----------|------|
| `welfare-ax-common/src/main/java/com/beplepay/welfareaxbe/common/exception/ErrorCode.java` | 수정 | EXTERNAL_API_ERROR 추가 |
| `welfare-ax-common/build.gradle.kts` | 확인 | RestClient 의존성 유효성 검토 |

---

## 상세 구현 가이드

### ErrorCode.java

기존 ErrorCode enum에 외부 API 오류 전용 코드를 추가한다.

```java
// 외부 API 호출 실패 — HTTP 4xx·5xx 응답 또는 타임아웃
EXTERNAL_API_ERROR("E101", "외부 API 호출에 실패했습니다.", HttpStatus.BAD_GATEWAY),
```

- 코드 범위: E1xx — 외부 연동 오류 전용 블록
- HTTP 상태: 502 BAD_GATEWAY (서버가 업스트림에서 잘못된 응답을 받았음)
- 기존 E001~E999 코드와 번호 충돌 없음 확인

### welfare-ax-common/build.gradle.kts

`spring-boot-starter-webmvc` 의존성에 `spring-web`이 포함되므로 `RestClient` 클래스는 이미 사용 가능하다.
`spring-boot-starter-restclient`가 Spring Boot 4.1.0에서 유효한 스타터인지 확인한다.

- 유효하면: `welfare-ax-common/build.gradle.kts`에 명시적으로 추가
- 유효하지 않으면: `welfare-ax-user`의 해당 의존성 제거, `spring-web`(webmvc 포함)으로 RestClient 사용 확인

> `JdkClientHttpRequestFactory` 는 `spring-web` 내 포함. Java 21 내장 `java.net.http.HttpClient` 사용이므로 별도 의존성 추가 불필요.

---

## 완료 기준

- [ ] `EXTERNAL_API_ERROR` ("E101", HttpStatus.BAD_GATEWAY) ErrorCode 추가
- [ ] `spring-boot-starter-restclient` 의존성 유효성 확인 및 처리 완료
- [ ] 기존 ErrorCode 단위 테스트 영향 없음 확인 (`GlobalExceptionHandlerTest` 통과)

---

## 다음 페이즈 연결

페이즈 2에서 `CommonHttpClient`가 `EXTERNAL_API_ERROR`를 사용하여 예외를 변환한다.
`RestClient` 클래스 가용성이 확인된 후 `HttpLoggingInterceptor`와 `CommonHttpClient`를 구현한다.
