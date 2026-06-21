package com.beplepay.welfareaxbe.user.client;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.beplepay.welfareaxbe.common.http.CommonHttpClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TestApiClient 통합 테스트 — httpbin.org 실 호출.
 *
 * <p>외부 네트워크가 필요하므로 {@code @Tag("integration")}으로 분리한다.
 * CI 환경에서 네트워크 불가 시 {@code -Dgroups=!integration}으로 제외할 수 있다.
 *
 * <p>Spring 컨텍스트 없이 CommonHttpClient와 TestApiClient를 직접 생성하여 실 호출을 검증한다.
 */
@Tag("integration")
class TestApiClientIntegrationTest {

    private TestApiClient testApiClient;

    @BeforeEach
    void setUp() {
        // application-api.yaml 설정값에 해당하는 값으로 직접 생성
        CommonHttpClient httpClient = new CommonHttpClient("https://httpbin.org", 3000, 5000);
        testApiClient = new TestApiClient(httpClient);
    }

    @Test
    void get_httpbin_성공() {
        Map<String, Object> response = testApiClient.get();

        assertThat(response).isNotNull();
        // httpbin.org /get 응답에는 요청한 url 필드가 포함된다
        assertThat(response.get("url")).asString().contains("/get");
    }

    @Test
    void post_httpbin_성공() {
        Map<String, Object> requestBody = Map.of("name", "welfare-ax", "version", "1");

        Map<String, Object> response = testApiClient.post(requestBody);

        assertThat(response).isNotNull();
        // httpbin.org /post 응답의 json 필드에 요청 본문이 에코된다
        assertThat(response).containsKey("json");
        @SuppressWarnings("unchecked")
        Map<String, Object> echoedJson = (Map<String, Object>) response.get("json");
        assertThat(echoedJson).containsEntry("name", "welfare-ax");
    }

    @Test
    void put_httpbin_성공() {
        Map<String, Object> requestBody = Map.of("status", "updated");

        Map<String, Object> response = testApiClient.put(requestBody);

        assertThat(response).isNotNull();
        assertThat(response).containsKey("json");
    }

    @Test
    void delete_httpbin_성공() {
        // 예외 없이 정상 완료되면 통과
        testApiClient.delete();
    }
}
