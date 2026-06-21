# 개발 브리프 — COMMON-00002

> 작성일: 2026-06-21
> 과업 번호: COMMON-00002
> 작성자: gkwns458

---

## 1. 과업 개요

### 목적
외부 API 호출을 위한 공통 HTTP 클라이언트(`CommonHttpClient`)를 구현한다.
Spring 6.1+의 `RestClient`를 기반으로 하며, HTTP 통신의 공통 관심사(로깅, 예외 처리, 타임아웃, 직렬화)를 일괄 처리한다.

### 배경
- 향후 경조사 지원 과업에서 외부 결제·알림·인증 API 호출이 필요해질 예정
- 각 API Client마다 중복 구현 없이 공통 HTTP 인프라를 재사용하기 위해 사전 구축
- `welfare-ax-user/build.gradle.kts`에 `spring-boot-starter-restclient` 의존성이 이미 선언되어 있으나 미구현 상태

---

## 2. 대상 도메인

- **모듈**: `welfare-ax-common` (라이브러리 모듈 — user·admin·batch 전체 재사용)
- **스코프**: `common`
- **테스트 클라이언트 위치**: `welfare-ax-user` (CommonHttpClient 활용 패턴 예시)

---

## 3. 기능 요구사항

### 3-1. CommonHttpClient (`welfare-ax-common`)

| # | 기능 | 상세 |
|---|------|------|
| F-01 | HTTP 메서드 지원 | GET, POST, PUT, DELETE |
| F-02 | 공통 로깅 | 요청: 메서드·URL·헤더 / 응답: 상태코드·헤더. Body 로깅은 선택 옵션(기본 OFF) |
| F-03 | 공통 예외 처리 | `RestClientException` → `WelfareException(ErrorCode.EXTERNAL_API_ERROR)` 변환 |
| F-04 | 타임아웃 처리 | connectTimeout·readTimeout을 생성 시점에 주입받아 설정 |
| F-05 | 직렬화 처리 | Jackson 기반 Request Body → JSON, Response JSON → 제네릭 타입 역직렬화 |
| F-06 | 인증 무관 설계 | Authorization Header·API Key·Basic Auth 생성 로직 포함 금지. 헤더는 호출부에서 Map으로 전달 |

### 3-2. application-api.yaml (설정 파일)

- 위치: 각 실행 모듈(`welfare-ax-user` 등) `src/main/resources/`
- 파일명: `application-api.yaml` (프로젝트 전체 `.yaml` 확장자 통일)
- API별 설정 항목: `base-url`, `connect-timeout`(ms), `read-timeout`(ms), `api-key` 등
- 예시 구조:
  ```yaml
  api:
    httpbin:
      base-url: https://httpbin.org
      connect-timeout: 3000
      read-timeout: 5000
  ```

### 3-3. TestApiClient (`welfare-ax-user`)

- httpbin.org를 대상으로 GET, POST, PUT, DELETE 각 엔드포인트 호출 구현
- `application-api.yaml`의 `api.httpbin.*` 설정을 읽어 `CommonHttpClient` 빈 구성
- 인증이 필요 없는 httpbin.org 특성상 헤더는 Content-Type만 전달

### 3-4. ErrorCode 추가 (`welfare-ax-common`)

- 외부 API 오류 전용 ErrorCode 추가:
  - `EXTERNAL_API_ERROR` — 외부 API 호출 실패 (HTTP 4xx·5xx 또는 타임아웃) → 502 BAD_GATEWAY

---

## 4. 비기능 요구사항

| 항목 | 기준 |
|------|------|
| 재사용성 | `welfare-ax-common`에 위치해 모든 실행 모듈에서 의존 가능 |
| 인증 분리 | `CommonHttpClient`는 인증 방식을 전혀 알지 못함. 헤더는 호출부 주입 |
| 설정 외부화 | URL·타임아웃·API Key 등은 코드가 아닌 `application-api.yaml`에서 관리 |
| 테스트 가능성 | `MockRestServiceServer` 또는 `WireMock` 기반 단위·통합 테스트 가능한 구조 |
| 보안 | API Key 등 민감 설정은 `application-api.yaml`에 명시하되 운영 환경 값은 환경변수로 오버라이드 |

---

## 5. 대상 API / 컴포넌트 목록

| 컴포넌트 | 모듈 | 경로 |
|----------|------|------|
| `CommonHttpClient` | `welfare-ax-common` | `com.beplepay.welfareaxbe.common.http` |
| `HttpLoggingInterceptor` | `welfare-ax-common` | `com.beplepay.welfareaxbe.common.http` |
| `ErrorCode` (EXTERNAL_API_ERROR 추가) | `welfare-ax-common` | `com.beplepay.welfareaxbe.common.exception` |
| `HttpbinProperties` | `welfare-ax-user` | `com.beplepay.welfareaxbe.user.config` |
| `TestApiClient` | `welfare-ax-user` | `com.beplepay.welfareaxbe.user.client` |
| `application-api.yaml` | `welfare-ax-user` | `src/main/resources/` |

---

## 6. DB 변경 사항

없음 — HTTP 클라이언트 공통 레이어는 DB 변경 없음.

---

## 7. 연동 대상

| 대상 | 용도 | 인증 |
|------|------|------|
| `https://httpbin.org` | 개발·테스트용 HTTP 에코 API | 없음 |

> 실제 외부 API 연동은 경조사·결제 과업에서 각 `ApiClient` 구현 시 추가

---

## 8. 보안 고려사항

- `CommonHttpClient`는 인증 헤더 생성 로직을 포함하지 않는다 (설계 강제)
- API Key 등 인증 정보는 `application-api.yaml`에 위치하며, 운영 환경은 환경변수(`API_HTTPBIN_API_KEY` 등)로 오버라이드
- 로깅 시 Authorization 헤더 값은 마스킹 처리 (`****` 로 대체)
- `application-api.yaml`은 운영 시크릿 포함 시 git 추적 제외 필요 (현재는 로컬 테스트 값만 포함)

---

## 9. 테스트 기준

### 단위 테스트 (`CommonHttpClientTest`)
- GET 요청 성공 → 응답 역직렬화 검증
- POST 요청 성공 → 요청 본문 직렬화 검증
- PUT / DELETE 요청 성공
- 4xx 응답 → `WelfareException(EXTERNAL_API_ERROR)` 발생
- 5xx 응답 → `WelfareException(EXTERNAL_API_ERROR)` 발생
- 타임아웃 → `WelfareException(EXTERNAL_API_ERROR)` 발생
- 로깅 인터셉터 — URL·메서드·상태코드 로그 검증

### 통합 테스트 (`TestApiClientIntegrationTest`)
- httpbin.org 실 호출: GET `/get`, POST `/post`, PUT `/put`, DELETE `/delete`
- 응답 상태 200 및 JSON 필드 검증

### 테스트 도구
- 단위: JUnit 5 + Mockito + `MockRestServiceServer`
- 통합: `@SpringBootTest` (profile: `local`, httpbin.org 실 호출)

---

## 10. 산출물 위치

| 산출물 | 경로 |
|-------|------|
| 개발 브리프 | `target/works/COMMON-00002_dev_brief.md` |
| 개발·테스트 계획서 | `target/plans/COMMON-00002/` |
| 테스트 결과서 | `target/test-reports/COMMON-00002_test_result.md` |

---

## 11. 미결 사항

| # | 항목 | 담당 | 비고 |
|---|------|------|------|
| M-01 | `welfare-ax-common/build.gradle.kts`에 RestClient 의존성 명시 필요 여부 확인 | 개발 | `spring-boot-starter-webmvc` 포함으로 사용 가능하나, `spring-boot-starter-restclient`가 별도 스타터인지 확인 |
| M-02 | 실제 외부 API 목록 미정 | 기획 | TestApiClient(httpbin.org)는 구조 검증용, 실 API는 경조사 과업에서 추가 |
| M-03 | Body 로깅 ON/OFF 설정 노출 여부 | 개발 | 기본 OFF, yaml 설정으로 활성화 가능하게 구현 권장 |

---

## 부록: 질의응답 로그

| 질문 | 답변 |
|------|------|
| HTTP 공통 레이어 모듈 위치 | `welfare-ax-common` |
| 외부 API 인증 방식 | `CommonHttpClient`는 인증 무관. 헤더는 호출부에서 생성·주입 |
| 타임아웃 처리 | yaml 설정 주입 방식. connectTimeout·readTimeout |
| 오류 처리 | `WelfareException(EXTERNAL_API_ERROR)` 변환, ErrorCode E1xx 추가 |
| 로깅 | Interceptor 기반 URL·헤더·상태코드 로깅. Authorization 마스킹 |
| 테스트 대상 | httpbin.org (`TestApiClient`) |
| 설정 파일 | `application-api.yaml` 별도 분리 |
