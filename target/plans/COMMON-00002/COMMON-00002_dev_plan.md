# 개발 계획서

| 항목 | 내용 |
|------|------|
| 과업번호 | COMMON-00002 |
| 제목 | HTTP 공통 클라이언트 구현 (CommonHttpClient) |
| 작성일 | 2026-06-21 |
| 작성자 | AI (검토: gkwns458) |
| 대상 스코프 | common |
| 브랜치 | feature/COMMON-00002/gkwns458 |

---

## 1. 과업 개요

Spring 6.1+의 `RestClient`를 기반으로 외부 API 호출을 위한 공통 HTTP 클라이언트(`CommonHttpClient`)를 구현한다.
HTTP 통신의 공통 관심사(로깅·예외 처리·타임아웃·직렬화)를 일괄 처리하며, 인증 방식은 알지 못하도록 설계한다.
호출부(`TestApiClient` 등)가 헤더를 생성하여 주입하는 구조로, 향후 경조사·결제·알림 API 클라이언트의 공통 인프라로 재사용된다.

---

## 2. 기능 요구사항

- [ ] F-01: GET, POST, PUT, DELETE HTTP 메서드 지원
- [ ] F-02: `ClientHttpRequestInterceptor` 기반 공통 로깅 (메서드·URL·헤더·상태코드, Authorization 마스킹)
- [ ] F-03: `RestClientException` → `WelfareException(EXTERNAL_API_ERROR)` 변환
- [ ] F-04: connectTimeout·readTimeout 생성 시점 주입 (ms 단위, `JdkClientHttpRequestFactory`)
- [ ] F-05: Jackson 기반 Request Body → JSON 직렬화 / Response JSON → 제네릭 타입 역직렬화
- [ ] F-06: 인증 무관 설계 — `CommonHttpClient` 내 Authorization·API Key·Basic Auth 생성 금지
- [ ] F-07: `EXTERNAL_API_ERROR` ErrorCode 추가 (502 BAD_GATEWAY)
- [ ] F-08: `application-api.yaml` 분리 및 `@ConfigurationProperties` 기반 설정 바인딩
- [ ] F-09: `TestApiClient` — httpbin.org 대상 GET·POST·PUT·DELETE 호출 구현

---

## 3. 대상 스코프 및 허용 경로

`common` 스코프 기준 허용 경로:

| 경로 | 비고 |
|------|------|
| `welfare-ax-common/src/main/java/**` | CommonHttpClient, HttpLoggingInterceptor, ErrorCode |
| `welfare-ax-common/src/test/java/**` | 단위 테스트 |
| `welfare-ax-user/src/main/java/.../user/config/**` | HttpbinProperties, TestApiClientConfig |
| `welfare-ax-user/src/main/resources/application.yaml` | api 프로파일 활성화 추가 |
| `welfare-ax-user/src/main/resources/application-local.yaml` | (변경 없음) |

> **스코프 외 경로 주의**
> `welfare-ax-user/src/main/java/.../user/client/**` (TestApiClient) 및
> `welfare-ax-user/src/main/resources/application-api.yaml` 은 현재 `common` 스코프 허용 경로에 미포함.
> `/develop` 실행 전 scope.yaml 에 해당 경로 추가가 필요하다. (`.claude/` 파일 수정 — 별도 승인 필요)

---

## 4. 페이즈 목록

| 페이즈 | 제목 | 대상 파일 | 완료 |
|--------|------|-----------|------|
| 1 | 공통 인프라 준비 | `ErrorCode.java`, `welfare-ax-common/build.gradle.kts` | [ ] |
| 2 | CommonHttpClient 구현 + 단위 테스트 | `HttpLoggingInterceptor.java`, `CommonHttpClient.java`, `CommonHttpClientTest.java`, `HttpLoggingInterceptorTest.java` | [ ] |
| 3 | TestApiClient 구현 + 통합 테스트 | `application-api.yaml`, `HttpbinProperties.java`, `TestApiClientConfig.java`, `TestApiClient.java`, `TestApiClientTest.java`, `TestApiClientIntegrationTest.java` | [ ] |

---

## 5. DB 변경사항

없음.

---

## 6. 공통 모듈 변경사항

| 파일 | 변경 유형 | 내용 |
|------|----------|------|
| `welfare-ax-common/.../exception/ErrorCode.java` | 수정 | `EXTERNAL_API_ERROR` (E101, 502) 추가 |
| `welfare-ax-common/.../http/CommonHttpClient.java` | 신규 | RestClient 기반 공통 HTTP 클라이언트 |
| `welfare-ax-common/.../http/HttpLoggingInterceptor.java` | 신규 | 요청·응답 로깅 인터셉터 |
| `welfare-ax-common/build.gradle.kts` | 확인/수정 | RestClient 의존성 명시 여부 검토 |

---

## 7. 보안 고려사항

- `CommonHttpClient`에 인증 헤더 생성 로직 포함 금지 (코드 리뷰 체크포인트)
- 로깅 시 `Authorization` 헤더 값 마스킹 (`****` 대체)
- `application-api.yaml`의 API Key 등 민감 값은 환경변수로 오버라이드 (`API_HTTPBIN_API_KEY` 등)
- `application-api.yaml`은 운영 시크릿 포함 시 `.gitignore` 추가 필요 (현재 로컬 테스트 값만 포함)

---

## 8. 위험 요소 및 대응

| 위험 | 가능성 | 대응 |
|------|--------|------|
| `spring-boot-starter-restclient`가 Spring Boot 4.1.0에 유효하지 않은 의존성일 수 있음 | 중 | Phase 1에서 의존성 유효성 확인. `spring-boot-starter-webmvc`에 포함된 RestClient로 대체 가능 |
| httpbin.org 외부 네트워크 연결 필요 | 저 | 통합 테스트는 `@Tag("integration")` 로 분리하여 오프라인 환경에서 단위 테스트만 실행 가능하도록 구성 |
| 스코프 외 경로(`client/**`, `application-api.yaml`) | 중 | `/develop` 전 scope.yaml 경로 추가 승인 필요 |
| 타임아웃 설정 방식 — `JdkClientHttpRequestFactory` vs `HttpComponentsClientHttpRequestFactory` | 저 | Java 21 기본 제공 `JdkClientHttpRequestFactory` 우선 사용. Apache HttpClient 의존성 추가 불필요 |

---

## 9. 테스트 전략

| 유형 | 도구 | 범위 |
|------|------|------|
| 단위 테스트 | JUnit 5 + Mockito + `MockRestServiceServer` | `CommonHttpClient`, `HttpLoggingInterceptor` |
| 슬라이스 테스트 | `MockRestServiceServer` | `TestApiClient` — httpbin.org 모킹 |
| 통합 테스트 | `@SpringBootTest(webEnvironment=NONE)` + httpbin.org 실 호출 | `TestApiClient` 전 메서드 검증 |

---

## 10. 예상 산출물

| 파일 경로 | 유형 |
|----------|------|
| `welfare-ax-common/src/main/java/.../common/exception/ErrorCode.java` | 수정 |
| `welfare-ax-common/src/main/java/.../common/http/CommonHttpClient.java` | 신규 |
| `welfare-ax-common/src/main/java/.../common/http/HttpLoggingInterceptor.java` | 신규 |
| `welfare-ax-common/src/test/java/.../common/http/CommonHttpClientTest.java` | 신규 |
| `welfare-ax-common/src/test/java/.../common/http/HttpLoggingInterceptorTest.java` | 신규 |
| `welfare-ax-user/src/main/resources/application-api.yaml` | 신규 |
| `welfare-ax-user/src/main/java/.../user/config/HttpbinProperties.java` | 신규 |
| `welfare-ax-user/src/main/java/.../user/config/TestApiClientConfig.java` | 신규 |
| `welfare-ax-user/src/main/java/.../user/client/TestApiClient.java` | 신규 |
| `welfare-ax-user/src/test/java/.../user/client/TestApiClientTest.java` | 신규 |
| `welfare-ax-user/src/test/java/.../user/client/TestApiClientIntegrationTest.java` | 신규 |
