# Phase 2 — HttpLoggingInterceptor traceId 전파

## 목표
CommonHttpClient로 외부 API를 호출할 때, MDC에 저장된 traceId를 읽어
외부 요청 헤더 `X-Trace-Id`에 자동으로 추가한다.

---

## 수정 파일

### `HttpLoggingInterceptor.java`

```
경로: welfare-ax-common/src/main/java/com/beplepay/welfareaxbe/common/http/HttpLoggingInterceptor.java
```

**변경 내용 — `intercept()` 메서드:**

```java
@Override
public ClientHttpResponse intercept(
        HttpRequest request, byte[] body, ClientHttpRequestExecution execution
) throws IOException {
    // MDC traceId가 있으면 외부 요청 헤더에 전파
    HttpRequest requestToExecute = propagateTraceId(request);
    logRequest(requestToExecute, body);
    ClientHttpResponse response = execution.execute(requestToExecute, body);
    BufferingClientHttpResponseWrapper buffered = new BufferingClientHttpResponseWrapper(response);
    logResponse(buffered);
    return buffered;
}
```

**추가 메서드 — `propagateTraceId()`:**

```java
private HttpRequest propagateTraceId(HttpRequest request) {
    String traceId = MDC.get("traceId");
    if (traceId == null) {
        return request;  // MDC에 traceId 없으면 원본 그대로 반환
    }
    // HttpRequestWrapper로 래핑하여 X-Trace-Id 헤더 추가
    return new HttpRequestWrapper(request) {
        @Override
        public HttpHeaders getHeaders() {
            HttpHeaders headers = new HttpHeaders();
            headers.putAll(super.getHeaders());
            headers.set("X-Trace-Id", traceId);
            return headers;
        }
    };
}
```

**주의:**
- `ClientHttpRequestInterceptor`의 `HttpRequest`는 불변 헤더를 가질 수 있으므로 `HttpRequestWrapper`로 래핑
- `MDC.get("traceId")` null 처리 → TraceIdFilter 미적용 환경에서도 안전

---

### `HttpLoggingInterceptorTest.java`

**추가 테스트:**

| 메서드명 | TC | 검증 내용 |
|---------|-----|-----------|
| `intercept_MDC_traceId있으면_X_Trace_Id헤더전파()` | TC05 | MDC traceId 설정 후 MockRestServiceServer에서 X-Trace-Id 헤더 검증 |
| `intercept_MDC_traceId없으면_X_Trace_Id헤더없음()` | TC06 | MDC 비어있을 때 X-Trace-Id 헤더 미포함 확인 |

**테스트 패턴:**
```java
@Test
void intercept_MDC_traceId있으면_X_Trace_Id헤더전파() {
    MDC.put("traceId", "test-trace-id-1234");
    try {
        server.expect(requestTo(BASE_URL + "/data"))
              .andExpect(header("X-Trace-Id", "test-trace-id-1234"))
              .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        client.get("/data", Map.of(), MAP_TYPE);
        server.verify();
    } finally {
        MDC.remove("traceId");
    }
}
```

---

## 완료 기준
- [ ] TC05·TC06 테스트 통과
- [ ] 기존 `HttpLoggingInterceptorTest` 8건 모두 회귀 없음
- [ ] `propagateTraceId()` Javadoc 한국어 작성
