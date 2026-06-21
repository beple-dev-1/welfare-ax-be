package com.beplepay.welfareaxbe.common.http;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.beplepay.welfareaxbe.common.exception.ErrorCode;
import com.beplepay.welfareaxbe.common.exception.WelfareException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * CommonHttpClient 단위 테스트.
 * MockRestServiceServer를 사용하여 실제 HTTP 통신 없이 검증한다.
 */
class CommonHttpClientTest {

    private static final String BASE_URL = "https://test.example.com";
    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<>() {};

    private MockRestServiceServer server;
    private CommonHttpClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        // MockRestServiceServer를 RestClient.Builder에 바인딩하여 실제 HTTP 요청을 가로챈다
        server = MockRestServiceServer.bindTo(builder).build();
        // forTesting() 팩토리 메서드로 MockRestServiceServer 기반 CommonHttpClient 생성
        client = CommonHttpClient.forTesting(builder.build());
    }

    @Test
    void get_성공_응답역직렬화() {
        server.expect(requestTo(BASE_URL + "/data"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"key": "value"}
                        """, MediaType.APPLICATION_JSON));

        Map<String, Object> result = client.get("/data", Map.of(), MAP_TYPE);

        assertThat(result).containsEntry("key", "value");
        server.verify();
    }

    @Test
    void post_성공_응답역직렬화() {
        server.expect(requestTo(BASE_URL + "/data"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {"result": "created"}
                        """, MediaType.APPLICATION_JSON));

        Map<String, Object> result = client.post("/data", Map.of("Content-Type", "application/json"),
                Map.of("name", "test"), MAP_TYPE);

        assertThat(result).containsEntry("result", "created");
        server.verify();
    }

    @Test
    void put_성공_응답역직렬화() {
        server.expect(requestTo(BASE_URL + "/data/1"))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withSuccess("""
                        {"result": "updated"}
                        """, MediaType.APPLICATION_JSON));

        Map<String, Object> result = client.put("/data/1", Map.of("Content-Type", "application/json"),
                Map.of("name", "updated"), MAP_TYPE);

        assertThat(result).containsEntry("result", "updated");
        server.verify();
    }

    @Test
    void delete_성공() {
        server.expect(requestTo(BASE_URL + "/data/1"))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withSuccess());

        client.delete("/data/1", Map.of());

        server.verify();
    }

    @Test
    void get_호출부헤더_요청에포함() {
        server.expect(requestTo(BASE_URL + "/data"))
                .andExpect(header("X-Custom-Header", "custom-value"))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        client.get("/data", Map.of("X-Custom-Header", "custom-value"), MAP_TYPE);

        server.verify();
    }

    @Test
    void get_4xx응답_WelfareException발생() {
        server.expect(requestTo(BASE_URL + "/data"))
                .andRespond(withStatus(org.springframework.http.HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> client.get("/data", Map.of(), MAP_TYPE))
                .isInstanceOf(WelfareException.class)
                .satisfies(ex -> {
                    WelfareException we = (WelfareException) ex;
                    assertThat(we.getErrorCode()).isEqualTo(ErrorCode.EXTERNAL_API_ERROR);
                });
    }

    @Test
    void get_5xx응답_WelfareException발생() {
        server.expect(requestTo(BASE_URL + "/data"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> client.get("/data", Map.of(), MAP_TYPE))
                .isInstanceOf(WelfareException.class)
                .satisfies(ex -> {
                    WelfareException we = (WelfareException) ex;
                    assertThat(we.getErrorCode()).isEqualTo(ErrorCode.EXTERNAL_API_ERROR);
                });
    }

    @Test
    void get_Class타입_응답역직렬화() {
        server.expect(requestTo(BASE_URL + "/text"))
                .andRespond(withSuccess("hello", MediaType.TEXT_PLAIN));

        String result = client.get("/text", Map.of(), String.class);

        assertThat(result).isEqualTo("hello");
        server.verify();
    }
}
