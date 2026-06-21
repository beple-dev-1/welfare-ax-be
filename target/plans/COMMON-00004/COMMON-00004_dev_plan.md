# 개발 계획서

| 항목 | 내용 |
|------|------|
| 과업번호 | COMMON-00004 |
| 제목 | traceId 상수화 및 Swagger(OpenAPI) 연계 |
| 작성일 | 2026-06-21 |
| 작성자 | AI (검토: gkwns458) |
| 대상 스코프 | common |
| 브랜치 | feature/COMMON-00004/gkwns458 |

---

## 1. 과업 개요

`TraceIdFilter`·`HttpLoggingInterceptor`에 하드코딩된 `"traceId"` 문자열을 `MdcConstants` 공통 상수 클래스로 일원화하고, `welfare-ax-user` 모듈에 Swagger(OpenAPI 3.0) UI와 JWT Bearer Authorize 버튼을 추가한다.

**배경**
- COMMON-00003 HANDOFF 미결사항: `"traceId"` 키가 두 클래스에 중복 하드코딩됨
- 향후 로그인 API 구현 시 Swagger에서 JWT 토큰을 즉시 테스트할 수 있도록 Authorize UI 사전 구성

---

## 2. 기능 요구사항

- [ ] FR-01: `MdcConstants` 신규 생성 (`common.util` 패키지, `TRACE_ID_KEY = "traceId"`)
- [ ] FR-02: `TraceIdFilter` — 내부 상수 제거, `MdcConstants.TRACE_ID_KEY` 참조로 교체
- [ ] FR-03: `HttpLoggingInterceptor` — `MDC.get("traceId")` → `MDC.get(MdcConstants.TRACE_ID_KEY)` 교체
- [ ] FR-04: `logback-spring.xml` — `%X{traceId:-NO_TRACE}` 옆에 MdcConstants 참조 주석 추가
- [ ] FR-05: `welfare-ax-user/build.gradle.kts` — springdoc-openapi 의존성 추가
- [ ] FR-06: `SwaggerConfig` 신규 생성 — OpenAPI 메타정보 + BearerAuth SecurityScheme + GlobalSecurity
- [ ] FR-07: `SecurityConfig` 수정 — `/swagger-ui/**`, `/swagger-ui.html`, `/v3/api-docs/**` → permitAll 추가
- [ ] FR-08: `application-local.yaml` — springdoc 활성화 설정 추가
- [ ] FR-09: `application-dev.yaml` 신규 생성 — springdoc 활성화 설정

---

## 3. 대상 스코프 및 허용 경로

스코프 `common` 기준 (`scope.yaml`):

| 경로 | 작업 | 스코프 내 여부 |
|------|------|--------------|
| `welfare-ax-common/src/main/java/**` | MdcConstants 신규, TraceIdFilter·HttpLoggingInterceptor 수정 | ✅ |
| `welfare-ax-common/src/test/java/**` | 단위 테스트 | ✅ |
| `welfare-ax-user/src/main/java/config/**` | SwaggerConfig 신규, SecurityConfig 수정 | ✅ |
| `welfare-ax-user/src/test/java/config/**` | 통합 테스트 | ⚠️ (test/config 미정의, 허용 처리) |
| `welfare-ax-user/src/main/resources/application-local.yaml` | springdoc 설정 추가 | ✅ |
| `welfare-ax-user/src/main/resources/application-dev.yaml` | 신규 생성 | ⚠️ 스코프 외 — 명시적 승인 필요 |
| `welfare-ax-user/build.gradle.kts` | springdoc 의존성 추가 | ⚠️ 스코프 외 — 명시적 승인 필요 |
| `welfare-ax-user/src/main/resources/logback-spring.xml` | 주석 추가 | ⚠️ 스코프 외 — 명시적 승인 필요 |

> **⚠️ 스코프 외 파일** (`build.gradle.kts`, `application-dev.yaml`, `logback-spring.xml`) 수정 전 담당자 승인 필요.

---

## 4. 페이즈 목록

| 페이즈 | 제목 | 대상 파일 | 완료 |
|--------|------|---------|------|
| 1 | traceId 상수화 | MdcConstants.java, TraceIdFilter.java, HttpLoggingInterceptor.java | [ ] |
| 2 | Swagger 인프라 구성 | build.gradle.kts, SwaggerConfig.java, SecurityConfig.java | [ ] |
| 3 | 환경별 설정 및 최종 검증 | application-local.yaml, application-dev.yaml, logback-spring.xml, 빌드 | [ ] |

---

## 5. DB 변경사항

**없음** — DDL 변경 없음.

---

## 6. 공통 모듈 변경사항

| 모듈 | 변경 내용 |
|------|---------|
| `welfare-ax-common` | `com.beplepay.welfareaxbe.common.util.MdcConstants` 신규 추가 |
| `welfare-ax-common` | `TraceIdFilter`: 내부 `TRACE_ID_KEY` 상수 제거 → `MdcConstants` 참조 |
| `welfare-ax-common` | `HttpLoggingInterceptor`: `"traceId"` 리터럴 → `MdcConstants.TRACE_ID_KEY` |

---

## 7. 보안 고려사항

- Swagger UI 경로 `permitAll`은 springdoc 비활성화 환경(운영)에서 404 응답하므로 이중 차단 효과
- `BearerAuth` SecurityScheme은 UI 편의 기능이며 실제 인가 처리는 `JwtFilter`에서 수행
- `logback-spring.xml` 주석 변경만이므로 보안 영향 없음

---

## 8. 위험 요소 및 대응

| 위험 | 대응 |
|------|------|
| springdoc-openapi 버전이 Spring Boot 4.1.0과 비호환 | Phase 2에서 `./gradlew dependencies` 로 의존성 충돌 즉시 확인 후 버전 조정 |
| `TraceIdFilter`의 package-private `TRACE_ID_KEY` 제거 시 기존 테스트 컴파일 오류 가능 | Phase 1 직후 `./gradlew :welfare-ax-common:test` 실행하여 회귀 확인 |
| `application-dev.yaml` 신규 생성 시 Spring Profile 그룹 등록 누락 | `application.yaml`의 `spring.profiles.group` 확인 후 dev 포함 여부 점검 |

---

## 9. 테스트 전략

| 유형 | 도구 | 대상 |
|------|------|------|
| 단위 | JUnit5 + Mockito | `MdcConstants` 상수값 검증, `TraceIdFilter` MDC 저장, `HttpLoggingInterceptor` traceId 전파 |
| 슬라이스 | `@WebMvcTest` + MockMvc | `GET /v3/api-docs` 200 OK, `GET /swagger-ui/**` 인증 없이 접근 가능 |
| 회귀 | 기존 30건 전체 | `./gradlew test` 전체 통과 확인 |

---

## 10. 예상 산출물

### 신규 생성
| 파일 | 위치 |
|------|------|
| `MdcConstants.java` | `welfare-ax-common/.../common/util/` |
| `SwaggerConfig.java` | `welfare-ax-user/.../user/config/` |
| `application-dev.yaml` | `welfare-ax-user/src/main/resources/` |

### 수정
| 파일 | 변경 내용 |
|------|---------|
| `TraceIdFilter.java` | 내부 상수 제거, MdcConstants 참조 |
| `HttpLoggingInterceptor.java` | `"traceId"` 리터럴 → `MdcConstants.TRACE_ID_KEY` |
| `SecurityConfig.java` | Swagger 경로 permitAll 추가 |
| `welfare-ax-user/build.gradle.kts` | springdoc 의존성 추가 |
| `application-local.yaml` | springdoc 활성화 설정 추가 |
| `logback-spring.xml` | traceId 패턴 옆 주석 추가 |

### 테스트
| 파일 |
|------|
| `MdcConstantsTest.java` |
| `TraceIdFilterTest.java` (기존 수정) |
| `SwaggerConfigTest.java` |
