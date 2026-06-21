# Phase 1 — TraceIdFilter 구현

## 목표
모든 인바운드 HTTP 요청에 UUID 기반 traceId를 부여하고 MDC에 저장한다.
응답 헤더 `X-Trace-Id`로 클라이언트에 반환하며, 요청 처리 완료 후 MDC를 정리한다.

---

## 구현 파일

### `TraceIdFilter.java`

```
경로: welfare-ax-common/src/main/java/com/beplepay/welfareaxbe/common/filter/TraceIdFilter.java
패키지: com.beplepay.welfareaxbe.common.filter
```

**클래스 구조:**
```java
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    private static final String TRACE_ID_KEY = "traceId";
    private static final String TRACE_ID_HEADER = "X-Trace-Id";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String traceId = UUID.randomUUID().toString();
        MDC.put(TRACE_ID_KEY, traceId);
        response.setHeader(TRACE_ID_HEADER, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            // 스레드 반환 전 MDC 정리 — 스레드풀 재사용 시 이전 traceId 누수 방지
            MDC.remove(TRACE_ID_KEY);
        }
    }
}
```

**주요 포인트:**
- `@Order(Ordered.HIGHEST_PRECEDENCE)` → 모든 필터 중 가장 먼저 실행
- `MDC.remove()` → `MDC.clear()` 대신 사용 (다른 MDC 항목 보존)
- `OncePerRequestFilter` → 요청당 1회 실행 보장 (포워드·인클루드 중복 방지)

---

### `TraceIdFilterTest.java`

```
경로: welfare-ax-common/src/test/java/com/beplepay/welfareaxbe/common/filter/TraceIdFilterTest.java
```

**테스트 목록:**

| 메서드명 | TC | 검증 내용 |
|---------|-----|-----------|
| `doFilterInternal_traceId_MDC저장및응답헤더설정()` | TC01 | MDC traceId 설정 + 응답 X-Trace-Id 헤더 |
| `doFilterInternal_요청완료후_MDC제거()` | TC02 | filterChain 실행 후 MDC 비어있음 |
| `doFilterInternal_예외발생시_MDC제거보장()` | TC03 | finally 블록으로 MDC 정리 보장 |
| `doFilterInternal_traceId_UUID형식()` | TC04 | UUID 정규식 패턴 일치 |

**테스트 도구:** `MockHttpServletRequest`, `MockHttpServletResponse`, `MockFilterChain` (spring-test)

---

## 완료 기준
- [ ] 4개 테스트 모두 통과
- [ ] `@Order(Ordered.HIGHEST_PRECEDENCE)` 적용 확인
- [ ] Javadoc (클래스·메서드) 한국어 작성 완료
