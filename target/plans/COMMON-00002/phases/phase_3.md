# 페이즈 3: TestApiClient 구현 + 통합 테스트

| 항목 | 내용 |
|------|------|
| 페이즈 번호 | 3 |
| 목표 | httpbin.org 대상 TestApiClient 구현 및 통합 테스트로 CommonHttpClient 동작 검증 |
| 의존 페이즈 | 2 (CommonHttpClient) |

---

## 구현 대상 파일

| 파일 경로 | 작업 유형 | 설명 |
|----------|----------|------|
| `welfare-ax-user/src/main/resources/application-api.yaml` | 신규 | API 설정 분리 파일 |
| `welfare-ax-user/src/main/java/com/beplepay/welfareaxbe/user/config/HttpbinProperties.java` | 신규 | httpbin 설정 바인딩 |
| `welfare-ax-user/src/main/java/com/beplepay/welfareaxbe/user/config/TestApiClientConfig.java` | 신규 | CommonHttpClient 빈 등록 설정 |
| `welfare-ax-user/src/main/java/com/beplepay/welfareaxbe/user/client/TestApiClient.java` | 신규 | httpbin.org API 호출 클라이언트 |
| `welfare-ax-user/src/test/java/com/beplepay/welfareaxbe/user/client/TestApiClientTest.java` | 신규 | MockRestServiceServer 기반 단위 테스트 |
| `welfare-ax-user/src/test/java/com/beplepay/welfareaxbe/user/client/TestApiClientIntegrationTest.java` | 신규 | httpbin.org 실 호출 통합 테스트 |

> **스코프 외 경로 사전 작업 필요**
> `welfare-ax-user/src/main/java/.../user/client/**` 및 `application-api.yaml`은 `common` 스코프 기본 허용 경로 외.
> `/develop` 실행 전 `scope.yaml`에 해당 경로 추가 승인 필요.

---

## 상세 구현 가이드

### application-api.yaml

```yaml
api:
  httpbin:
    base-url: https://httpbin.org
    connect-timeout: 3000   # 연결 타임아웃 (밀리초)
    read-timeout: 5000      # 읽기 타임아웃 (밀리초)
```

- Spring 프로파일 활성화: `application.yaml`의 `spring.profiles.active`에 `api` 추가
- 운영 환경 민감값은 환경변수 오버라이드: `API_HTTPBIN_BASE_URL` 등
- 파일명 규칙: 프로젝트 전체 `.yaml` 확장자 통일 준수

---

### HttpbinProperties.java

```java
/**
 * application-api.yaml의 api.httpbin 설정을 바인딩하는 프로퍼티 클래스.
 */
@ConfigurationProperties(prefix = "api.httpbin")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class HttpbinProperties {

    /** httpbin.org 기본 URL */
    private String baseUrl;

    /** 연결 타임아웃 (밀리초) */
    private long connectTimeout;

    /** 읽기 타임아웃 (밀리초) */
    private long readTimeout;
}
```

- `@EnableConfigurationProperties(HttpbinProperties.class)` 또는 `@ConfigurationPropertiesScan` 활성화 필요

---

### TestApiClientConfig.java

```java
/**
 * TestApiClient에서 사용할 CommonHttpClient 빈을 등록한다.
 *
 * <p>application-api.yaml의 httpbin 설정을 읽어 타임아웃을 구성한다.
 */
@Configuration
@EnableConfigurationProperties(HttpbinProperties.class)
public class TestApiClientConfig {

    /**
     * httpbin.org 전용 CommonHttpClient 빈을 생성한다.
     *
     * @param properties httpbin 연결 설정
     * @return CommonHttpClient 인스턴스
     */
    @Bean
    public CommonHttpClient httpbinHttpClient(HttpbinProperties properties) {
        return new CommonHttpClient(
            properties.getBaseUrl(),
            properties.getConnectTimeout(),
            properties.getReadTimeout()
        );
    }
}
```

---

### TestApiClient.java

```java
/**
 * httpbin.org를 대상으로 하는 테스트용 API 클라이언트.
 *
 * <p>CommonHttpClient의 사용 패턴을 검증하며, 실제 외부 API 클라이언트 구현의 참조 예시 역할을 한다.
 * 인증이 필요 없는 httpbin.org 특성상 Content-Type 헤더만 전달한다.
 */
@Component
@RequiredArgsConstructor
public class TestApiClient {

    private final CommonHttpClient httpbinHttpClient;

    private static final Map<String, String> DEFAULT_HEADERS = Map.of(
        "Content-Type", "application/json"
    );

    /**
     * httpbin.org GET /get 엔드포인트를 호출한다.
     *
     * @return httpbin 응답 (요청 정보 에코)
     */
    public Map<String, Object> get() {
        return httpbinHttpClient.get("/get", DEFAULT_HEADERS, responseType());
    }

    /**
     * httpbin.org POST /post 엔드포인트를 호출한다.
     *
     * @param body 요청 본문
     * @return httpbin 응답 (요청 정보 에코)
     */
    public Map<String, Object> post(Map<String, Object> body) {
        return httpbinHttpClient.post("/post", DEFAULT_HEADERS, body, responseType());
    }

    /**
     * httpbin.org PUT /put 엔드포인트를 호출한다.
     *
     * @param body 요청 본문
     * @return httpbin 응답
     */
    public Map<String, Object> put(Map<String, Object> body) {
        return httpbinHttpClient.put("/put", DEFAULT_HEADERS, body, responseType());
    }

    /**
     * httpbin.org DELETE /delete 엔드포인트를 호출한다.
     */
    public void delete() {
        httpbinHttpClient.delete("/delete", DEFAULT_HEADERS);
    }

    @SuppressWarnings("unchecked")
    private Class<Map<String, Object>> responseType() {
        return (Class<Map<String, Object>>) (Class<?>) Map.class;
    }
}
```

---

### TestApiClientTest.java (단위 테스트)

`MockRestServiceServer` 기반. httpbin.org 실 호출 없이 `CommonHttpClient` + `TestApiClient` 연동 검증.

테스트케이스:
- `get_성공_응답반환`
- `post_성공_body직렬화및응답반환`
- `put_성공_body직렬화및응답반환`
- `delete_성공`

---

### TestApiClientIntegrationTest.java (통합 테스트)

httpbin.org 실 호출. 네트워크 환경이 필요하므로 `@Tag("integration")`로 분리.

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles({"local", "api"})
@Tag("integration")
class TestApiClientIntegrationTest {
    @Autowired
    private TestApiClient testApiClient;

    @Test void get_httpbin_성공() { ... }    // 응답 url 필드 "/get" 포함 확인
    @Test void post_httpbin_성공() { ... }   // 응답 json 필드에 요청 body 포함 확인
    @Test void put_httpbin_성공() { ... }
    @Test void delete_httpbin_성공() { ... }
}
```

---

## 완료 기준

- [ ] `application-api.yaml` 생성 및 `application.yaml` 프로파일 활성화 연결
- [ ] `HttpbinProperties` 설정 바인딩 정상 동작
- [ ] `TestApiClientConfig` — httpbin 전용 `CommonHttpClient` 빈 등록 완료
- [ ] `TestApiClient` GET·POST·PUT·DELETE 구현 완료
- [ ] 단위 테스트 4건 (`TestApiClientTest`) GREEN
- [ ] 통합 테스트 4건 (`TestApiClientIntegrationTest`) GREEN (네트워크 환경)
- [ ] `TestApiClient` 내 인증 헤더 생성 코드 없음 확인

---

## 다음 페이즈 연결

이 페이즈 완료 후 `/qa-test COMMON-00002`로 테스트 결과서를 작성한다.
이후 `/code-review` → `/git commit` → `/git push` 순으로 진행한다.
