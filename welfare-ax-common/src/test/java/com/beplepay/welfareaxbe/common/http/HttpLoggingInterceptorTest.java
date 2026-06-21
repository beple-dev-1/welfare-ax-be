package com.beplepay.welfareaxbe.common.http;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

/**
 * HttpLoggingInterceptor 단위 테스트.
 */
@ExtendWith(MockitoExtension.class)
class HttpLoggingInterceptorTest {

    @Mock
    private ClientHttpRequestExecution execution;

    private HttpLoggingInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new HttpLoggingInterceptor();
    }

    @Test
    void intercept_정상실행_응답반환() throws IOException {
        // 응답 모킹: 200 OK
        MockClientHttpResponse mockResponse =
                new MockClientHttpResponse(new ByteArrayInputStream("{}".getBytes()), HttpStatus.OK);
        given(execution.execute(any(), any())).willReturn(mockResponse);

        MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.GET, URI.create("https://example.com/test"));
        request.getHeaders().set("Content-Type", "application/json");

        ClientHttpResponse result = interceptor.intercept(request, new byte[0], execution);

        // execution.execute()의 반환값이 그대로 반환되어야 한다
        assertThat(result).isEqualTo(mockResponse);
    }

    @Test
    void maskSensitiveHeaders_Authorization헤더마스킹() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer secret-token-value");
        headers.set("Content-Type", "application/json");

        HttpHeaders masked = interceptor.maskSensitiveHeaders(headers);

        // Authorization 헤더 값은 ****로 대체되어야 한다
        assertThat(masked.getFirst("Authorization")).isEqualTo("****");
        // 일반 헤더는 원본 값이 유지되어야 한다
        assertThat(masked.getFirst("Content-Type")).isEqualTo("application/json");
    }

    @Test
    void maskSensitiveHeaders_Authorization없으면마스킹없음() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        headers.set("X-Request-Id", "abc123");

        HttpHeaders masked = interceptor.maskSensitiveHeaders(headers);

        assertThat(masked.getFirst("Content-Type")).isEqualTo("application/json");
        assertThat(masked.getFirst("X-Request-Id")).isEqualTo("abc123");
    }

    @Test
    void maskSensitiveHeaders_Authorization대소문자무관마스킹() {
        HttpHeaders headers = new HttpHeaders();
        // 헤더명 대소문자 변형
        headers.set("authorization", "Basic dXNlcjpwYXNz");

        HttpHeaders masked = interceptor.maskSensitiveHeaders(headers);

        assertThat(masked.getFirst("authorization")).isEqualTo("****");
    }
}
