package com.beplepay.welfareaxbe.common.http;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.util.StreamUtils;

import com.beplepay.welfareaxbe.common.util.MdcConstants;

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

    @AfterEach
    void tearDown() {
        // 테스트 간 MDC 오염 방지
        MDC.remove(MdcConstants.TRACE_ID_KEY);
    }

    @Test
    void intercept_응답버퍼링_body재읽기가능() throws IOException {
        String responseBody = "{\"result\": \"ok\"}";
        MockClientHttpResponse mockResponse = new MockClientHttpResponse(
                new ByteArrayInputStream(responseBody.getBytes()), HttpStatus.OK);
        mockResponse.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        given(execution.execute(any(), any())).willReturn(mockResponse);

        MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.GET, URI.create("https://example.com/test"));
        request.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        ClientHttpResponse result = interceptor.intercept(request, new byte[0], execution);

        // 버퍼링된 응답에서 Body를 읽을 수 있어야 한다
        String body = StreamUtils.copyToString(result.getBody(), StandardCharsets.UTF_8);
        assertThat(body).isEqualTo(responseBody);
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void isBinaryContent_텍스트타입_false반환() {
        // application/json, text/*, application/xml, application/x-www-form-urlencoded는 텍스트 타입
        assertThat(interceptor.isBinaryContent(MediaType.APPLICATION_JSON)).isFalse();
        assertThat(interceptor.isBinaryContent(MediaType.TEXT_PLAIN)).isFalse();
        assertThat(interceptor.isBinaryContent(MediaType.TEXT_HTML)).isFalse();
        assertThat(interceptor.isBinaryContent(MediaType.APPLICATION_XML)).isFalse();
        assertThat(interceptor.isBinaryContent(MediaType.APPLICATION_FORM_URLENCODED)).isFalse();
    }

    @Test
    void isBinaryContent_바이너리타입_true반환() {
        // image/*, application/octet-stream은 바이너리 타입
        assertThat(interceptor.isBinaryContent(MediaType.IMAGE_PNG)).isTrue();
        assertThat(interceptor.isBinaryContent(MediaType.IMAGE_JPEG)).isTrue();
        assertThat(interceptor.isBinaryContent(MediaType.APPLICATION_OCTET_STREAM)).isTrue();
    }

    @Test
    void isBinaryContent_multipart타입_true반환() {
        // multipart/form-data는 파일 업로드 혼합 타입으로 바이너리로 처리한다
        assertThat(interceptor.isBinaryContent(MediaType.MULTIPART_FORM_DATA)).isTrue();
        assertThat(interceptor.isBinaryContent(MediaType.MULTIPART_MIXED)).isTrue();
    }

    @Test
    void isBinaryContent_null_false반환() {
        assertThat(interceptor.isBinaryContent(null)).isFalse();
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

    @Test
    void intercept_MDC_traceId있으면_X_Trace_Id헤더전파() throws IOException {
        MDC.put(MdcConstants.TRACE_ID_KEY, "test-trace-id-1234");

        MockClientHttpResponse mockResponse = new MockClientHttpResponse(
                new ByteArrayInputStream("{}".getBytes()), HttpStatus.OK);
        mockResponse.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        // execution.execute()에 전달된 HttpRequest를 캡처
        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        given(execution.execute(requestCaptor.capture(), any())).willReturn(mockResponse);

        MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.GET, URI.create("https://example.com/api"));
        interceptor.intercept(request, new byte[0], execution);

        // 외부 요청 헤더에 X-Trace-Id가 MDC traceId 값으로 추가되어야 한다
        assertThat(requestCaptor.getValue().getHeaders().getFirst("X-Trace-Id"))
                .isEqualTo("test-trace-id-1234");
    }

    @Test
    void intercept_MDC_traceId없으면_X_Trace_Id헤더없음() throws IOException {
        // MDC에 traceId 없는 상태 (TraceIdFilter 미적용 환경)
        MDC.remove(MdcConstants.TRACE_ID_KEY);

        MockClientHttpResponse mockResponse = new MockClientHttpResponse(
                new ByteArrayInputStream("{}".getBytes()), HttpStatus.OK);
        mockResponse.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        ArgumentCaptor<HttpRequest> requestCaptor = ArgumentCaptor.forClass(HttpRequest.class);
        given(execution.execute(requestCaptor.capture(), any())).willReturn(mockResponse);

        MockClientHttpRequest request = new MockClientHttpRequest(HttpMethod.GET, URI.create("https://example.com/api"));
        interceptor.intercept(request, new byte[0], execution);

        // MDC에 traceId가 없으면 외부 요청 헤더에 X-Trace-Id가 추가되지 않아야 한다
        assertThat(requestCaptor.getValue().getHeaders().getFirst("X-Trace-Id")).isNull();
    }
}
