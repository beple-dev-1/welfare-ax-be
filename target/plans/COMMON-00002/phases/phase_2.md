# 페이즈 2: CommonHttpClient 구현 + 단위 테스트

| 항목 | 내용 |
|------|------|
| 페이즈 번호 | 2 |
| 목표 | RestClient 기반 공통 HTTP 클라이언트 및 로깅 인터셉터 구현, 단위 테스트 작성 |
| 의존 페이즈 | 1 (EXTERNAL_API_ERROR ErrorCode, RestClient 의존성) |

---

## 구현 대상 파일

| 파일 경로 | 작업 유형 | 설명 |
|----------|----------|------|
| `welfare-ax-common/src/main/java/com/beplepay/welfareaxbe/common/http/HttpLoggingInterceptor.java` | 신규 | 요청·응답 로깅 인터셉터 |
| `welfare-ax-common/src/main/java/com/beplepay/welfareaxbe/common/http/CommonHttpClient.java` | 신규 | RestClient 기반 공통 HTTP 클라이언트 |
| `welfare-ax-common/src/test/java/com/beplepay/welfareaxbe/common/http/HttpLoggingInterceptorTest.java` | 신규 | 로깅 인터셉터 단위 테스트 |
| `welfare-ax-common/src/test/java/com/beplepay/welfareaxbe/common/http/CommonHttpClientTest.java` | 신규 | HTTP 클라이언트 단위 테스트 |

---

## 상세 구현 가이드

### HttpLoggingInterceptor.java

`ClientHttpRequestInterceptor` 구현체. `CommonHttpClient` 생성 시 `RestClient`에 등록된다.

```java
/**
 * RestClient 요청·응답 공통 로깅 인터셉터.
 *
 * <p>요청: HTTP 메서드, URI, 헤더 (Authorization 마스킹)
 * <p>응답: HTTP 상태 코드
 */
public class HttpLoggingInterceptor implements ClientHttpRequestInterceptor {

    private static final Logger log = LoggerFactory.getLogger(HttpLoggingInterceptor.class);
    private static final String MASKED = "****";

    @Override
    public ClientHttpResponse intercept(
            HttpRequest request, byte[] body, ClientHttpRequestExecution execution
    ) throws IOException {
        // 요청 로깅
        logRequest(request);
        ClientHttpResponse response = execution.execute(request, body);
        // 응답 로깅
        logResponse(response);
        return response;
    }

    private void logRequest(HttpRequest request) {
        // Authorization 헤더 마스킹 후 로깅
    }

    private void logResponse(ClientHttpResponse response) throws IOException {
        // 상태코드 로깅
    }

    /** Authorization 헤더 값을 마스킹한다. */
    private HttpHeaders maskSensitiveHeaders(HttpHeaders headers) {
        // Authorization 헤더가 있으면 값을 "****"로 대체
    }
}
```

**주의사항:**
- Response Body를 읽으면 스트림이 소비되어 이후 역직렬화 불가 → Body 로깅은 기본 OFF, 구현하지 않음
- `execution.execute()` 이후 응답을 반드시 반환해야 함

---

### CommonHttpClient.java

외부 API 호출을 담당하는 공통 HTTP 클라이언트. `@Component`가 아닌 일반 클래스로 구현하며, 각 API 설정에 따라 호출부(`TestApiClientConfig` 등)에서 `new CommonHttpClient(baseUrl, connectTimeout, readTimeout)`으로 생성하여 빈으로 등록한다.

```java
/**
 * RestClient 기반 공통 HTTP 클라이언트.
 *
 * <p>GET·POST·PUT·DELETE 메서드를 지원하며 로깅·예외 처리·타임아웃·직렬화를 공통 처리한다.
 * 인증 헤더(Authorization, API Key 등)는 이 클래스에서 생성하지 않는다.
 * 헤더는 호출부에서 {@code Map<String, String>}으로 전달한다.
 *
 * @param baseUrl        외부 API 기본 URL
 * @param connectTimeout 연결 타임아웃 (밀리초)
 * @param readTimeout    읽기 타임아웃 (밀리초)
 */
public class CommonHttpClient {

    private final RestClient restClient;

    public CommonHttpClient(String baseUrl, long connectTimeout, long readTimeout) {
        // JdkClientHttpRequestFactory + 타임아웃 설정
        // HttpLoggingInterceptor 등록
        // baseUrl 설정
    }

    /**
     * GET 요청을 수행하고 응답을 역직렬화하여 반환한다.
     *
     * @param path         요청 경로 (baseUrl 기준 상대 경로)
     * @param headers      요청 헤더 (인증 헤더 포함 가능)
     * @param responseType 응답 역직렬화 대상 타입
     * @return 역직렬화된 응답 객체
     * @throws WelfareException 외부 API 호출 실패 시 EXTERNAL_API_ERROR
     */
    public <T> T get(String path, Map<String, String> headers, Class<T> responseType) { ... }

    /**
     * POST 요청을 수행하고 응답을 역직렬화하여 반환한다.
     *
     * @param path         요청 경로
     * @param headers      요청 헤더
     * @param body         요청 본문 (Jackson 직렬화)
     * @param responseType 응답 역직렬화 대상 타입
     * @return 역직렬화된 응답 객체
     * @throws WelfareException 외부 API 호출 실패 시 EXTERNAL_API_ERROR
     */
    public <T> T post(String path, Map<String, String> headers, Object body, Class<T> responseType) { ... }

    /**
     * PUT 요청을 수행하고 응답을 역직렬화하여 반환한다.
     */
    public <T> T put(String path, Map<String, String> headers, Object body, Class<T> responseType) { ... }

    /**
     * DELETE 요청을 수행한다.
     *
     * @throws WelfareException 외부 API 호출 실패 시 EXTERNAL_API_ERROR
     */
    public void delete(String path, Map<String, String> headers) { ... }

    /** RestClientException을 WelfareException으로 변환한다. */
    private WelfareException toWelfareException(RestClientException e) {
        return new WelfareException(ErrorCode.EXTERNAL_API_ERROR, e.getMessage());
    }
}
```

**구현 시 주의사항:**
- `JdkClientHttpRequestFactory` 생성: `HttpClient.newBuilder().connectTimeout(...).build()` 후 팩토리에 readTimeout 설정
- `RestClientResponseException` (4xx·5xx), `ResourceAccessException` (타임아웃·연결 오류) 모두 `EXTERNAL_API_ERROR`로 변환
- 헤더 주입: `headers.forEach((k, v) -> spec.header(k, v))`
- 제네릭 역직렬화: `ParameterizedTypeReference` 활용 고려

---

### CommonHttpClientTest.java

`MockRestServiceServer` 기반 단위 테스트.

```java
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {/* 필요 최소 설정 */})
class CommonHttpClientTest {
    // 또는 MockRestServiceServer 직접 생성 방식 검토
}
```

테스트케이스:
- `get_성공_응답역직렬화` — 200 응답 → DTO 매핑 확인
- `post_성공_요청직렬화` — 요청 Body JSON 직렬화 확인
- `put_성공` — 200 응답 확인
- `delete_성공` — 응답 없음 처리 확인
- `get_4xx응답_WelfareException발생` — EXTERNAL_API_ERROR 코드 확인
- `get_5xx응답_WelfareException발생` — EXTERNAL_API_ERROR 코드 확인
- `get_타임아웃_WelfareException발생` — ResourceAccessException → WelfareException 변환 확인

### HttpLoggingInterceptorTest.java

- `요청로깅_Authorization마스킹` — Authorization 헤더 값이 `****`로 로깅되는지 확인
- `응답로깅_상태코드기록` — 상태코드 로그 출력 확인
- `인터셉터_응답반환정상` — `execution.execute()` 반환값이 그대로 반환되는지 확인

---

## 완료 기준

- [ ] `CommonHttpClient` GET·POST·PUT·DELETE 구현 완료
- [ ] `HttpLoggingInterceptor` Authorization 마스킹 포함 구현 완료
- [ ] `CommonHttpClient`에 인증 헤더 생성 코드 없음 확인
- [ ] 단위 테스트 7건 (`CommonHttpClientTest`) GREEN
- [ ] 단위 테스트 3건 (`HttpLoggingInterceptorTest`) GREEN

---

## 다음 페이즈 연결

페이즈 3에서 `CommonHttpClient`를 `TestApiClientConfig`에서 빈으로 등록하고 `TestApiClient`가 이를 주입받아 httpbin.org를 호출한다.
