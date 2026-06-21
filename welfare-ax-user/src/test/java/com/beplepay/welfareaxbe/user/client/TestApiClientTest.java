package com.beplepay.welfareaxbe.user.client;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.beplepay.welfareaxbe.common.http.CommonHttpClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * TestApiClient 단위 테스트.
 * MockRestServiceServer로 httpbin.org 응답을 모킹하여 실제 네트워크 없이 검증한다.
 */
class TestApiClientTest {

    private static final String BASE_URL = "https://httpbin.org";

    private MockRestServiceServer server;
    private TestApiClient testApiClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        // forTesting() 팩토리 메서드로 MockRestServiceServer 기반 CommonHttpClient 생성 후 TestApiClient에 주입
        CommonHttpClient httpClient = CommonHttpClient.forTesting(builder.build());
        testApiClient = new TestApiClient(httpClient);
    }

    @Test
    void get_성공_응답반환() {
        server.expect(requestTo(BASE_URL + "/get"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"url": "https://httpbin.org/get"}
                        """, MediaType.APPLICATION_JSON));

        Map<String, Object> result = testApiClient.get();

        assertThat(result).containsKey("url");
        server.verify();
    }

    @Test
    void post_성공_응답반환() {
        server.expect(requestTo(BASE_URL + "/post"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {"json": {"name": "test"}}
                        """, MediaType.APPLICATION_JSON));

        Map<String, Object> result = testApiClient.post(Map.of("name", "test"));

        assertThat(result).containsKey("json");
        server.verify();
    }

    @Test
    void put_성공_응답반환() {
        server.expect(requestTo(BASE_URL + "/put"))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withSuccess("""
                        {"json": {"name": "updated"}}
                        """, MediaType.APPLICATION_JSON));

        Map<String, Object> result = testApiClient.put(Map.of("name", "updated"));

        assertThat(result).containsKey("json");
        server.verify();
    }

    @Test
    void delete_성공() {
        server.expect(requestTo(BASE_URL + "/delete"))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withSuccess());

        testApiClient.delete();

        server.verify();
    }
}
