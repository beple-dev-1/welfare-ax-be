# 개발 브리프 — COMMON-00003

| 항목 | 내용 |
|------|------|
| 과업번호 | COMMON-00003 |
| 작성일 | 2026-06-21 |
| 스코프 | common |
| 브랜치 | feature/COMMON-00003/gkwns458 |

---

## 1. 과업 개요

### 목적
모든 HTTP 요청에 고유한 traceId를 부여하여 로그 추적성을 확보한다.
운영 환경에서 특정 요청의 전체 처리 흐름(인바운드 요청 → 비즈니스 로직 → 외부 API 호출)을 단일 traceId로 연결하여 빠른 장애 분석을 지원한다.

### 배경
- COMMON-00002에서 HTTP 공통 로깅(HttpLoggingInterceptor)이 구현됨
- 현재 로그에 요청 식별자가 없어 동시 다중 요청 환경에서 특정 요청의 로그를 구분할 수 없음
- 클라이언트로부터 X-Trace-Id를 수신하지 않고 서버에서 직접 생성 (보안 및 신뢰성)

---

## 2. 대상 도메인

`common` — 모든 실행 모듈(user, admin, batch)에서 공통으로 사용

---

## 3. 기능 요구사항

| # | 요구사항 | 구현 위치 |
|---|---------|-----------|
| F1 | 모든 인바운드 HTTP 요청 진입 시 UUID 기반 traceId 생성 | TraceIdFilter |
| F2 | 생성된 traceId를 MDC(`traceId` 키)에 저장 | TraceIdFilter |
| F3 | 응답 헤더 `X-Trace-Id`에 traceId 포함하여 클라이언트 반환 | TraceIdFilter |
| F4 | 요청 처리 완료 후 finally 블록에서 MDC 제거 | TraceIdFilter |
| F5 | CommonHttpClient 외부 API 호출 시 MDC에서 traceId를 읽어 요청 헤더 `X-Trace-Id`로 전파 | HttpLoggingInterceptor |
| F6 | logback 패턴에 traceId 포함 (`%X{traceId}`) | logback-spring.xml |

---

## 4. 비기능 요구사항

| 항목 | 기준 |
|------|------|
| 성능 | UUID 생성 및 MDC 조작은 O(1) — 요청 처리 지연 없음 |
| 안전성 | MDC 누수 방지 — finally 블록에서 반드시 제거 |
| 재사용성 | TraceIdFilter는 welfare-ax-common에 위치, 모든 실행 모듈에서 공용 |
| 독립성 | Spring Security 필터 체인과 무관하게 독립 서블릿 필터로 동작 |

---

## 5. 대상 파일 목록

### 신규 파일

| 파일 | 모듈 | 설명 |
|------|------|------|
| `common/filter/TraceIdFilter.java` | welfare-ax-common | 핵심 구현체 |
| `common/filter/TraceIdFilterTest.java` | welfare-ax-common (test) | 단위 테스트 |
| `resources/logback-spring.xml` | welfare-ax-user | traceId 포함 로그 패턴 |

### 수정 파일

| 파일 | 모듈 | 변경 내용 |
|------|------|-----------|
| `common/http/HttpLoggingInterceptor.java` | welfare-ax-common | MDC traceId → 외부 요청 헤더 전파 |
| `common/http/HttpLoggingInterceptorTest.java` | welfare-ax-common (test) | traceId 전파 테스트 추가 |

---

## 6. 상세 구현 설계

### TraceIdFilter

```
패키지: com.beplepay.welfareaxbe.common.filter
클래스: TraceIdFilter extends OncePerRequestFilter
어노테이션: @Component, @Order(Ordered.HIGHEST_PRECEDENCE)
MDC 키: "traceId"
응답 헤더: "X-Trace-Id"
```

```
doFilterInternal() {
    traceId = UUID.randomUUID().toString()
    MDC.put("traceId", traceId)
    response.setHeader("X-Trace-Id", traceId)
    try {
        filterChain.doFilter(request, response)
    } finally {
        MDC.remove("traceId")
    }
}
```

### HttpLoggingInterceptor 변경

```
intercept() {
    traceId = MDC.get("traceId")
    if (traceId != null) {
        request = HttpRequestWrapper(request) { 헤더에 X-Trace-Id 추가 }
    }
    logRequest(request, body)
    ...
}
```

### logback-spring.xml 패턴

```
[%d{yyyy-MM-dd HH:mm:ss.SSS}] [%thread] [%X{traceId:-NO_TRACE}] %-5level %logger{36} - %msg%n
```

---

## 7. 연동 대상

| 대상 | 연동 방식 |
|------|-----------|
| CommonHttpClient | HttpLoggingInterceptor를 통해 자동 전파 (기존 인터셉터 수정) |
| JwtFilter | TraceIdFilter 이후 실행 (`@Order(HIGHEST_PRECEDENCE)` 보장) |
| logback | `%X{traceId}` MDC 참조 |

---

## 8. 보안 고려사항

- 클라이언트가 전송한 `X-Trace-Id` 헤더는 **무시**한다 (서버에서 신규 생성만)
- traceId는 내부 추적용 UUID이므로 민감 정보 아님 — 응답 헤더 노출 허용
- MDC 누수 방지: 예외 발생 시에도 finally 블록 보장

---

## 9. 테스트 기준

| TC | 시나리오 | 기대 결과 |
|----|---------|-----------|
| TC01 | 일반 GET 요청 | MDC에 traceId 설정, 응답 헤더 X-Trace-Id 존재 |
| TC02 | 요청 처리 후 MDC 상태 | MDC에서 traceId 제거됨 |
| TC03 | 예외 발생 요청 | finally 실행으로 MDC 정리 보장 |
| TC04 | traceId 형식 | UUID 형식(8-4-4-4-12) 검증 |
| TC05 | HttpLoggingInterceptor | MDC traceId 있을 때 외부 요청 헤더에 X-Trace-Id 포함 |
| TC06 | HttpLoggingInterceptor | MDC traceId 없을 때 외부 요청 헤더에 X-Trace-Id 미포함 |

---

## 10. 산출물 위치

| 산출물 | 경로 |
|-------|------|
| 개발 브리프 | `target/works/COMMON-00003_dev_brief.md` |
| 개발 계획서 | `target/plans/COMMON-00003/` |
| 테스트 결과서 | `target/test-reports/COMMON-00003_test_result.md` |

---

## 11. 미결 사항

없음 — 모든 설계 결정 완료

---

## 부록: 질의응답 로그

| 질문 | 답변 |
|------|------|
| 클라이언트 X-Trace-Id 헤더 수신 여부 | 수신하지 않음, 서버에서 UUID 생성 |
| traceId 응답 헤더 반환 | 반환 (X-Trace-Id) |
| CommonHttpClient 외부 전파 | 전파 (X-Trace-Id 헤더) |
| logback-spring.xml 작성 | 함께 작성 |
| 필터 등록 방식 | @Component 독립 서블릿 필터 (@Order(HIGHEST_PRECEDENCE)) |
