# 테스트 계획서

| 항목 | 내용 |
|------|------|
| 과업번호 | COMMON-00004 |
| 제목 | traceId 상수화 및 Swagger(OpenAPI) 연계 테스트 계획 |
| 작성일 | 2026-06-21 |
| 대응 개발 계획서 | COMMON-00004_dev_plan.md |

---

## 1. 테스트 범위

| 기능 | 테스트 포함 여부 |
|------|---------------|
| `MdcConstants.TRACE_ID_KEY` 상수값 검증 | ✅ |
| `TraceIdFilter` MDC 저장 및 정리 (상수 교체 후 회귀) | ✅ |
| `HttpLoggingInterceptor` traceId 전파 (상수 교체 후 회귀) | ✅ |
| Swagger UI `/swagger-ui/**` 접근 허용 (인증 없이 200) | ✅ |
| OpenAPI Docs `/v3/api-docs` 스펙 반환 | ✅ |
| `BearerAuth` SecurityScheme 포함 여부 | ✅ |
| springdoc 비활성화 시 UI 미노출 | ✅ |
| 전체 빌드 회귀 (기존 30건) | ✅ |

---

## 2. 테스트 방법

| 유형 | 도구 | 범위 |
|------|------|------|
| 단위 테스트 | JUnit5 + Mockito | MdcConstants, TraceIdFilter, HttpLoggingInterceptor |
| 슬라이스 테스트 | `@WebMvcTest` + MockMvc | SwaggerConfig (OpenAPI 스펙, Swagger 경로 접근) |
| 수동 검증 | 브라우저 | Swagger UI 렌더링, Authorize 버튼 동작 |

---

## 3. 테스트 환경

- JUnit 5 (`spring-boot-starter-test`)
- Mockito (`@ExtendWith(MockitoExtension.class)`)
- `@WebMvcTest` — Spring Security 포함 슬라이스 테스트
- 빌드: `./gradlew test`

---

## 4. 테스트케이스 목록

### Phase 1 — traceId 상수화

| TC 번호 | 제목 | 유형 | 우선순위 | 완료 |
|--------|------|------|---------|------|
| TC-04-COMMON-001 | MdcConstants.TRACE_ID_KEY 값이 "traceId"인지 검증 | 단위 | 높음 | [ ] |
| TC-04-COMMON-002 | TraceIdFilter 실행 후 MDC에 traceId 저장 확인 | 단위 | 높음 | [ ] |
| TC-04-COMMON-003 | TraceIdFilter 실행 완료 후 MDC에서 traceId 제거 확인 | 단위 | 높음 | [ ] |
| TC-04-COMMON-004 | TraceIdFilter 응답 헤더 X-Trace-Id 설정 확인 | 단위 | 중간 | [ ] |
| TC-04-COMMON-005 | HttpLoggingInterceptor — MDC traceId 있을 때 X-Trace-Id 헤더 전파 | 단위 | 높음 | [ ] |
| TC-04-COMMON-006 | HttpLoggingInterceptor — MDC traceId 없을 때 원본 요청 그대로 반환 | 단위 | 중간 | [ ] |

### Phase 2 — Swagger 인프라

| TC 번호 | 제목 | 유형 | 우선순위 | 완료 |
|--------|------|------|---------|------|
| TC-04-SWAGGER-001 | GET /v3/api-docs → 200 OK + JSON 반환 | 슬라이스 | 높음 | [ ] |
| TC-04-SWAGGER-002 | OpenAPI 스펙에 components.securitySchemes.BearerAuth 존재 | 슬라이스 | 높음 | [ ] |
| TC-04-SWAGGER-003 | OpenAPI 스펙에 global security BearerAuth 적용 확인 | 슬라이스 | 높음 | [ ] |
| TC-04-SWAGGER-004 | GET /swagger-ui/index.html — 인증 없이 200 OK (permitAll) | 슬라이스 | 높음 | [ ] |
| TC-04-SWAGGER-005 | GET /swagger-ui.html — 인증 없이 200 OK (리다이렉트 포함) | 슬라이스 | 중간 | [ ] |
| TC-04-SWAGGER-006 | API 메타정보 — title, version, contact 설정값 검증 | 슬라이스 | 낮음 | [ ] |

### Phase 3 — 회귀 테스트

| TC 번호 | 제목 | 유형 | 우선순위 | 완료 |
|--------|------|------|---------|------|
| TC-04-REG-001 | 기존 JwtFilterTest 전체 통과 | 회귀 | 높음 | [ ] |
| TC-04-REG-002 | 기존 TraceIdFilterTest 전체 통과 | 회귀 | 높음 | [ ] |
| TC-04-REG-003 | 기존 HttpLoggingInterceptorTest 전체 통과 | 회귀 | 높음 | [ ] |
| TC-04-REG-004 | ./gradlew clean build BUILD SUCCESSFUL | 회귀 | 높음 | [ ] |

---

## 5. TC 상세 명세

### TC-04-COMMON-001: MdcConstants.TRACE_ID_KEY 상수값 검증

```
클래스: MdcConstantsTest
메서드: traceIdKey_shouldBeTraceId()
방법: assertEquals("traceId", MdcConstants.TRACE_ID_KEY)
```

### TC-04-COMMON-002: TraceIdFilter — MDC traceId 저장 확인

```
클래스: TraceIdFilterTest (기존 수정)
메서드: doFilter_shouldPutTraceIdToMdc()
방법: MockHttpServletRequest/Response + FilterChain mock
       doFilterInternal 실행 중 MDC.get(MdcConstants.TRACE_ID_KEY) != null 검증
       (FilterChain 내부에서 MDC 값 캡처)
```

### TC-04-COMMON-003: TraceIdFilter — MDC traceId 제거 확인

```
메서드: doFilter_shouldRemoveTraceIdFromMdcAfterFilter()
방법: doFilterInternal 완료 후 MDC.get(MdcConstants.TRACE_ID_KEY) == null 검증
```

### TC-04-SWAGGER-001: GET /v3/api-docs → 200 OK

```
클래스: SwaggerConfigTest
어노테이션: @WebMvcTest + @Import(SecurityConfig.class, SwaggerConfig.class)
방법: mockMvc.perform(get("/v3/api-docs"))
       .andExpect(status().isOk())
       .andExpect(jsonPath("$.openapi").exists())
```

### TC-04-SWAGGER-002: BearerAuth SecurityScheme 존재 확인

```
방법: mockMvc.perform(get("/v3/api-docs"))
       .andExpect(jsonPath("$.components.securitySchemes.BearerAuth").exists())
       .andExpect(jsonPath("$.components.securitySchemes.BearerAuth.type").value("http"))
       .andExpect(jsonPath("$.components.securitySchemes.BearerAuth.scheme").value("bearer"))
```

### TC-04-SWAGGER-004: /swagger-ui/index.html 인증 없이 접근

```
방법: mockMvc.perform(get("/swagger-ui/index.html"))
       .andExpect(status().isOk()) 또는 3xx 리다이렉트 허용
       (401/403 이 아닌 것으로 permitAll 검증)
```

---

## 6. 완료 기준 (DoD)

### 기능 검증
- [ ] 전체 TC GREEN (통과)
- [ ] 정상 케이스 (Happy Path) TC 포함 — `/v3/api-docs` 정상 반환
- [ ] 경계값 케이스 TC 포함 — MDC traceId 없을 때 HttpLoggingInterceptor 처리
- [ ] 예외 케이스 TC 포함 — 필터 실행 후 MDC 정리

### 복지AX 도메인 필수 검증
- [ ] 지급 중복 방지: 해당 없음 (인프라 변경)
- [ ] 잔액 부족: 해당 없음
- [ ] 권한 없는 접근 거부: Swagger 경로 permitAll 확인으로 대체
- [ ] 미등록 가맹점: 해당 없음

### 코드 품질
- [ ] `/code-review` 실행 후 CRITICAL 0건
- [ ] `MdcConstants`, `TraceIdFilter`, `HttpLoggingInterceptor` 핵심 메서드 단위 테스트 커버리지 확인

### 보안
- [ ] PII 로그 노출 없음 — logback 변경 내용 검토 (주석만 추가, 패턴 변경 없음)
- [ ] 인증 없이 접근 가능한 엔드포인트: Swagger 경로만 추가 허용 (의도적)
- [ ] 운영 환경 springdoc 비활성화 설정 (`enabled: false`) 문서화 확인
