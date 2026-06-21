# COMMON-00004 개발 브리프

> 작성일: 2026-06-21
> 도메인: common
> 스코프: welfare-ax-common, welfare-ax-user

---

## 1. 과업 개요

**목적**
- `"traceId"` 하드코딩 문자열을 `MdcConstants` 상수 클래스로 일원화하여 오타·불일치 위험 제거
- Swagger(OpenAPI 3.0) UI를 `welfare-ax-user` 모듈에 추가하여 API 문서를 자동 생성
- JWT Bearer 인증 스키마를 Swagger Authorize 버튼으로 미리 구성 (로그인 API 구현 전 사전 준비)

**배경**
- `TraceIdFilter`·`HttpLoggingInterceptor` 두 클래스에 `"traceId"` 키가 각각 하드코딩되어 있음 (HANDOFF 미결사항)
- 향후 로그인 API 구현 시 Swagger에서 토큰을 즉시 테스트할 수 있도록 Authorize UI 사전 제공
- `logback-spring.xml`의 `%X{traceId:-NO_TRACE}` 패턴은 Java 상수를 직접 참조할 수 없으므로 문자열 유지, 주석으로 연결 명시

---

## 2. 대상 도메인

| 모듈 | 변경 여부 | 내용 |
|------|----------|------|
| `welfare-ax-common` | **변경** | `MdcConstants` 추가, `TraceIdFilter`·`HttpLoggingInterceptor` 상수 참조로 교체 |
| `welfare-ax-user` | **변경** | springdoc 의존성 추가, `SwaggerConfig` 추가, `SecurityConfig` Swagger 경로 허용 |
| `welfare-ax-domain` | 없음 | — |
| `welfare-ax-admin` | 없음 | 향후 별도 과업에서 Swagger 추가 |
| `welfare-ax-batch` | 없음 | — |

---

## 3. 기능 요구사항

### FR-01. traceId 상수화

| 항목 | 내용 |
|------|------|
| 신규 클래스 | `com.beplepay.welfareaxbe.common.util.MdcConstants` |
| 상수 | `public static final String TRACE_ID_KEY = "traceId"` |
| 변경 대상 | `TraceIdFilter` — `"traceId"` 리터럴 → `MdcConstants.TRACE_ID_KEY` |
| 변경 대상 | `HttpLoggingInterceptor` — `MDC.get("traceId")` → `MDC.get(MdcConstants.TRACE_ID_KEY)` |
| 유지 | `logback-spring.xml` `%X{traceId:-NO_TRACE}` — 문자열 유지, 주석으로 MdcConstants 연결 명시 |

### FR-02. Swagger(OpenAPI) 연계

| 항목 | 내용 |
|------|------|
| 의존성 추가 모듈 | `welfare-ax-user/build.gradle.kts` |
| 라이브러리 | `springdoc-openapi-starter-webmvc-ui` (Spring Boot 4.x 호환 버전) |
| 설정 클래스 | `com.beplepay.welfareaxbe.user.config.SwaggerConfig` |
| API 메타정보 | title: `welfare-ax-user API`, version: `0.0.1`, contact: beplepay |
| Swagger UI 경로 | `/swagger-ui/index.html` |
| API Docs 경로 | `/v3/api-docs` |

### FR-03. Swagger Authorize 버튼 (JWT Bearer)

| 항목 | 내용 |
|------|------|
| SecurityScheme 이름 | `BearerAuth` |
| 타입 | HTTP |
| Scheme | `bearer` |
| Bearer Format | `JWT` |
| Global Security | 전체 API에 기본 적용 (`globalSecurity`) |
| 동작 | UI에서 토큰 입력 시 모든 API 요청에 `Authorization: Bearer {token}` 자동 추가 |

### FR-04. SecurityConfig 수정

| 항목 | 내용 |
|------|------|
| 추가 공개 경로 | `/swagger-ui/**`, `/swagger-ui.html`, `/v3/api-docs/**` |
| 활성화 환경 | local, dev 프로파일 (운영에서도 경로는 열려있으나 UI 비활성화로 접근 차단) |

### FR-05. Swagger UI 환경별 활성화

| 프로파일 | springdoc.swagger-ui.enabled | springdoc.api-docs.enabled |
|---------|------------------------------|---------------------------|
| local | true | true |
| dev | true | true |
| 그 외 (prod 등) | false (미설정 시 기본값) | false |

---

## 4. 비기능 요구사항

- **보안**: 운영 환경에서 Swagger UI는 비활성화(`springdoc.swagger-ui.enabled=false`) 처리
- **성능**: springdoc 의존성은 `welfare-ax-user`에만 국한, 공통 모듈 classpath 불필요
- **유지보수**: `MdcConstants.TRACE_ID_KEY` 한 곳만 수정하면 filter·interceptor·향후 코드 전체 반영

---

## 5. 대상 API / 화면 목록

| 항목 | URL | 설명 |
|------|-----|------|
| Swagger UI | `/swagger-ui/index.html` | API 문서 화면 |
| OpenAPI Docs | `/v3/api-docs` | JSON 스펙 |
| OpenAPI Docs YAML | `/v3/api-docs.yaml` | YAML 스펙 |

---

## 6. DB 변경사항

**없음** — 이번 과업은 코드·설정 변경만이며 DDL 변경 없음.

---

## 7. 연동 대상

**없음** — 외부 API, 내부 서비스 연동 없음.

---

## 8. 보안 고려사항

- Swagger UI 경로(`/swagger-ui/**`, `/v3/api-docs/**`)는 SecurityConfig에서 `permitAll()` 처리하되, 운영 환경은 `springdoc.swagger-ui.enabled=false`로 UI 자체를 비활성화하여 이중 차단
- Swagger Authorize 버튼은 UI 편의 기능이며, 실제 인가 검증은 `JwtFilter`·`SecurityConfig`에서 수행 (Swagger 설정과 무관)
- `MdcConstants`는 `public` 상수 클래스이며 민감 정보 없음

---

## 9. 테스트 기준

| ID | 분류 | 시나리오 | 기대결과 |
|----|------|---------|---------|
| TC-01 | 단위 | `TraceIdFilter` 실행 후 MDC에 traceId 저장 확인 | `MDC.get(MdcConstants.TRACE_ID_KEY)` != null |
| TC-02 | 단위 | `HttpLoggingInterceptor.propagateTraceId()` 호출 시 헤더에 `X-Trace-Id` 설정 | 헤더 값 == MDC traceId |
| TC-03 | 단위 | `MdcConstants.TRACE_ID_KEY` 값 == `"traceId"` | 상수 값 검증 |
| TC-04 | 통합 | `GET /v3/api-docs` 요청 시 200 OK + JSON 반환 | openapi 필드 포함 |
| TC-05 | 통합 | `GET /swagger-ui/index.html` 요청 시 200 OK | HTML 페이지 반환 |
| TC-06 | 통합 | OpenAPI 스펙에 `BearerAuth` securityScheme 포함 여부 | components.securitySchemes.BearerAuth 존재 |
| TC-07 | 통합 | 인증 없이 `/swagger-ui/**` 접근 시 401 미반환 | 200 OK (permitAll) |
| TC-08 | 회귀 | 기존 JwtFilter·TraceIdFilter 동작 정상 여부 | 기존 30건 테스트 통과 |

---

## 10. 산출물 위치

| 산출물 | 경로 |
|--------|------|
| 개발 브리프 | `target/works/COMMON-00004_dev_brief.md` |
| 개발 계획서 | `target/plans/COMMON-00004/` |
| 테스트 결과서 | `target/test-reports/COMMON-00004_test_result.md` |
| 신규 파일 | `welfare-ax-common/.../common/util/MdcConstants.java` |
| 신규 파일 | `welfare-ax-user/.../user/config/SwaggerConfig.java` |
| 변경 파일 | `welfare-ax-common/.../common/filter/TraceIdFilter.java` |
| 변경 파일 | `welfare-ax-common/.../common/http/HttpLoggingInterceptor.java` |
| 변경 파일 | `welfare-ax-user/.../user/config/SecurityConfig.java` |
| 변경 파일 | `welfare-ax-user/build.gradle.kts` |
| 변경 파일 | `welfare-ax-user/src/main/resources/application-local.yaml` |

---

## 11. 미결 사항

| ID | 항목 | 내용 |
|----|------|------|
| OPEN-01 | springdoc 버전 | Spring Boot 4.1.0 호환 springdoc-openapi 정확한 버전 확인 필요 (빌드 후 의존성 충돌 검증) |
| OPEN-02 | dev 프로파일 | `application-dev.yaml` 파일 존재 여부 미확인 — 없으면 신규 생성 필요 |
| OPEN-03 | admin/batch logback | admin·batch 모듈의 logback-spring.xml 동기화 (별도 과업으로 분리) |
| OPEN-04 | 비동기 MDC 전파 | `@Async` 환경 `MDCTaskDecorator` 적용 (별도 과업으로 분리) |

---

## 부록: 질의응답 로그

| 질문 | 답변 |
|------|------|
| Swagger 설정 모듈 위치 | `welfare-ax-user`에만 (실행 모듈별 독립 설정 원칙) |
| Swagger UI 활성화 환경 | local + dev 프로파일 |
| MdcConstants 패키지 위치 | `com.beplepay.welfareaxbe.common.util` |
| Swagger UI 접근 보안 | SecurityConfig에서 `/swagger-ui/**`, `/v3/api-docs/**` permitAll 추가 |
| API 메타정보 | 기본값 (title: welfare-ax-user API, version: 0.0.1, contact: beplepay) |
| JWT 헤더 형식 | Bearer 스키마 (`Authorization: Bearer {token}`) |

---

## 자가점검

- [x] 트랜잭션 경계: 해당 없음 (코드·설정 변경만)
- [x] 권한·접근 제어: Swagger 경로 permitAll + 운영 UI 비활성화로 이중 차단
- [x] 공통(common) vs 실행모듈(user) 분리 계획: MdcConstants → common, SwaggerConfig → user
- [x] 중복 처리 방지: 해당 없음
- [x] 입력값 검증: 해당 없음
- [x] 배치 재처리: 해당 없음
