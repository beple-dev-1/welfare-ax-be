# 테스트 계획서 — COMMON-00003

| 항목 | 내용 |
|------|------|
| 과업번호 | COMMON-00003 |
| 작성일 | 2026-06-21 |
| 테스트 대상 | TraceIdFilter, HttpLoggingInterceptor (traceId 전파) |

---

## 테스트 전략

| 레이어 | 도구 | 범위 |
|--------|------|------|
| 단위 (TraceIdFilter) | JUnit5 + Mockito + MockMvc 슬라이스 | 필터 동작, MDC 관리 |
| 단위 (HttpLoggingInterceptor) | JUnit5 + MockRestServiceServer | 헤더 전파 |

---

## TC 목록

### TraceIdFilterTest (Phase 1)

| TC | 메서드명 | 시나리오 | 기대 결과 | 우선순위 |
|----|---------|---------|-----------|----------|
| TC01 | `doFilterInternal_traceId_MDC저장및응답헤더설정` | 일반 GET 요청 진입 | MDC `traceId` 설정됨, 응답 헤더 `X-Trace-Id` 존재 | 필수 |
| TC02 | `doFilterInternal_요청완료후_MDC제거` | filterChain.doFilter() 완료 후 | `MDC.get("traceId")` == null | 필수 |
| TC03 | `doFilterInternal_예외발생시_MDC제거보장` | filterChain에서 RuntimeException 발생 | finally 블록 실행 → MDC 비어있음 | 필수 |
| TC04 | `doFilterInternal_traceId_UUID형식` | traceId 생성 | UUID 정규식(`[0-9a-f-]{36}`) 일치 | 필수 |

### HttpLoggingInterceptorTest 추가 (Phase 2)

| TC | 메서드명 | 시나리오 | 기대 결과 | 우선순위 |
|----|---------|---------|-----------|----------|
| TC05 | `intercept_MDC_traceId있으면_X_Trace_Id헤더전파` | MDC에 traceId 설정 후 외부 API 호출 | 외부 요청 헤더에 `X-Trace-Id: {traceId}` 포함 | 필수 |
| TC06 | `intercept_MDC_traceId없으면_X_Trace_Id헤더없음` | MDC 비어있는 상태에서 외부 API 호출 | 외부 요청 헤더에 `X-Trace-Id` 없음 | 필수 |

---

## 회귀 테스트

| 대상 | 테스트 수 | 확인 사항 |
|------|-----------|-----------|
| 기존 HttpLoggingInterceptorTest | 8건 | Phase 2 변경 후 기존 테스트 전부 통과 |
| 기존 CommonHttpClientTest | 8건 | TraceId 전파 추가 후 기존 테스트 영향 없음 |

---

## 실행 명령

```bash
# TraceIdFilter 테스트
./gradlew :welfare-ax-common:test --tests "*.TraceIdFilterTest"

# HttpLoggingInterceptor 전체 테스트 (기존 + 신규)
./gradlew :welfare-ax-common:test --tests "*.HttpLoggingInterceptorTest"

# welfare-ax-common 전체
./gradlew :welfare-ax-common:test
```

---

## 완료 기준

- [ ] TC01 ~ TC06 전체 통과
- [ ] 기존 HttpLoggingInterceptorTest 8건 회귀 없음
- [ ] 기존 CommonHttpClientTest 8건 회귀 없음
- [ ] `./gradlew :welfare-ax-common:test` BUILD SUCCESSFUL
