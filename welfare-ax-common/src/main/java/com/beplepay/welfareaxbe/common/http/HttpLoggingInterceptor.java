package com.beplepay.welfareaxbe.common.http;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.support.HttpRequestWrapper;
import org.springframework.util.StreamUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.beplepay.welfareaxbe.common.util.MdcConstants;

/**
 * RestClient 요청·응답 공통 로깅 인터셉터.
 *
 * <p>로그 레벨 정책:
 * <ul>
 *   <li>DEBUG — 메서드, URI, 헤더 (Authorization 마스킹). 운영 환경에서 기록 허용.
 *   <li>TRACE — Body 전문 추가 기록. 운영 환경에서 반드시 비활성화해야 한다.
 *               Body에 PII(이름·연락처·계좌번호 등)가 포함될 수 있으므로
 *               {@code logging.level.com.beplepay.welfareaxbe=INFO} 이상으로 설정한다.
 * </ul>
 *
 * <p>바이너리 Content-Type(image/*, multipart/*, application/octet-stream 등)은
 * Body를 {@value BINARY_PLACEHOLDER}로 대체하여 로그 파일 오염을 방지한다.
 * Body 크기가 {@value MAX_BODY_LOG_BYTES}바이트를 초과하면 크기 정보만 기록한다.
 */
public class HttpLoggingInterceptor implements ClientHttpRequestInterceptor {

    private static final Logger log = LoggerFactory.getLogger(HttpLoggingInterceptor.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String MASKED = "****";
    private static final String AUTHORIZATION = "Authorization";
    private static final String BINARY_PLACEHOLDER = "<binary>";

    /** Body 로그 최대 크기 (64KB 초과 시 크기 정보만 기록) */
    private static final int MAX_BODY_LOG_BYTES = 64 * 1024;

    /**
     * 요청 전후로 JSON 로깅을 수행하고 실제 요청을 실행한다.
     * 응답 Body 스트림은 버퍼링하여 로깅 후 원본 호출자가 재읽기 가능하도록 한다.
     *
     * @param request   HTTP 요청 정보
     * @param body      요청 바이트 배열
     * @param execution 실제 요청 실행기
     * @return 버퍼링된 HTTP 응답 (Body 재읽기 가능)
     * @throws IOException 요청 실행 중 I/O 오류 발생 시
     */
    @Override
    public ClientHttpResponse intercept(
            HttpRequest request, byte[] body, ClientHttpRequestExecution execution
    ) throws IOException {
        // MDC traceId가 있으면 외부 요청 헤더에 전파
        HttpRequest requestToExecute = propagateTraceId(request);
        logRequest(requestToExecute, body);
        ClientHttpResponse response = execution.execute(requestToExecute, body);
        // 응답 Body 로깅 후 호출자가 재읽기 가능하도록 스트림을 버퍼링
        BufferingClientHttpResponseWrapper buffered = new BufferingClientHttpResponseWrapper(response);
        logResponse(buffered);
        return buffered;
    }

    /**
     * MDC에 저장된 traceId를 외부 요청 헤더 {@code X-Trace-Id}에 추가한다.
     * MDC에 traceId가 없으면 원본 요청을 그대로 반환한다.
     *
     * <p>{@link org.springframework.http.client.ClientHttpRequestInterceptor}의 {@code HttpRequest}는
     * 불변 헤더를 가질 수 있으므로 {@link HttpRequestWrapper}로 래핑하여 헤더를 추가한다.
     *
     * @param request 원본 외부 HTTP 요청
     * @return traceId가 헤더에 추가된 요청 (traceId 없으면 원본 반환)
     */
    private HttpRequest propagateTraceId(HttpRequest request) {
        String traceId = MDC.get(MdcConstants.TRACE_ID_KEY);
        // MDC에 traceId가 없으면 TraceIdFilter 미적용 환경 — 원본 요청 그대로 사용
        if (traceId == null) {
            return request;
        }
        return new HttpRequestWrapper(request) {
            // 헤더 객체를 한 번만 생성하여 getHeaders() 반복 호출 시 불필요한 재생성을 방지
            private final HttpHeaders propagated = buildPropagated();

            private HttpHeaders buildPropagated() {
                HttpHeaders h = new HttpHeaders();
                h.putAll(super.getHeaders());
                // 인바운드에서 수신한 traceId를 아웃바운드 요청으로 전파
                h.set("X-Trace-Id", traceId);
                return HttpHeaders.readOnlyHttpHeaders(h);
            }

            @Override
            public HttpHeaders getHeaders() {
                return propagated;
            }
        };
    }

    /**
     * 요청 정보를 JSON 형식으로 기록한다.
     * 메서드·URI·헤더는 DEBUG, Body는 TRACE 레벨로 분리하여 운영 환경 PII 노출을 방지한다.
     *
     * @param request HTTP 요청 정보
     * @param body    요청 바이트 배열
     */
    private void logRequest(HttpRequest request, byte[] body) {
        try {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("type", "HTTP_REQUEST");
            entry.put("method", request.getMethod().name());
            entry.put("uri", request.getURI().toString());
            entry.put("headers", flattenHeaders(maskSensitiveHeaders(request.getHeaders())));
            // 메서드·URI·헤더는 DEBUG로 기록 (운영 환경에서도 허용)
            log.debug(OBJECT_MAPPER.writeValueAsString(entry));
            // Body는 TRACE로 격리 — 운영 환경에서 반드시 비활성화해야 한다
            if (log.isTraceEnabled() && body != null && body.length > 0) {
                entry.put("body", buildBodyLog(body, request.getHeaders().getContentType()));
                log.trace(OBJECT_MAPPER.writeValueAsString(entry));
            }
        } catch (Exception e) {
            log.debug("[HTTP 요청 로깅 실패]", e);
        }
    }

    /**
     * 응답 정보를 JSON 형식으로 기록한다.
     * 상태 코드·헤더는 DEBUG, Body는 TRACE 레벨로 분리하여 운영 환경 PII 노출을 방지한다.
     *
     * @param response 버퍼링된 HTTP 응답
     */
    private void logResponse(BufferingClientHttpResponseWrapper response) {
        try {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("type", "HTTP_RESPONSE");
            entry.put("status", response.getStatusCode().value());
            entry.put("headers", flattenHeaders(response.getHeaders()));
            // 상태 코드·헤더는 DEBUG로 기록 (운영 환경에서도 허용)
            log.debug(OBJECT_MAPPER.writeValueAsString(entry));
            // Body는 TRACE로 격리 — 운영 환경에서 반드시 비활성화해야 한다
            byte[] responseBody = response.getBodyBytes();
            if (log.isTraceEnabled() && responseBody != null && responseBody.length > 0) {
                entry.put("body", buildBodyLog(responseBody, response.getHeaders().getContentType()));
                log.trace(OBJECT_MAPPER.writeValueAsString(entry));
            }
        } catch (Exception e) {
            log.debug("[HTTP 응답 로깅 실패]", e);
        }
    }

    /**
     * Body 바이트 배열을 로그용 문자열로 변환한다.
     * 바이너리 타입이면 {@value BINARY_PLACEHOLDER}, 크기 초과 시 크기 정보만 반환한다.
     *
     * @param body        Body 바이트 배열
     * @param contentType Content-Type (null 허용)
     * @return 로그에 기록할 Body 문자열
     */
    private String buildBodyLog(byte[] body, MediaType contentType) {
        if (isBinaryContent(contentType)) {
            return BINARY_PLACEHOLDER;
        }
        // 64KB 초과 시 크기 정보만 기록하여 로그 파일 과부하를 방지한다
        if (body.length > MAX_BODY_LOG_BYTES) {
            return "<body too large: " + body.length + " bytes>";
        }
        return new String(body, StandardCharsets.UTF_8);
    }

    /**
     * Content-Type이 바이너리인지 판단한다.
     * text/*, application/json, application/xml, application/x-www-form-urlencoded는 텍스트로 간주한다.
     * multipart/*는 바이너리 혼합 타입으로 처리한다.
     *
     * @param mediaType 판단할 Content-Type (null이면 false 반환)
     * @return 바이너리 Content-Type이면 true
     */
    boolean isBinaryContent(MediaType mediaType) {
        if (mediaType == null) return false;
        if ("text".equals(mediaType.getType())) return false;
        // multipart/form-data, multipart/mixed 등 파일 업로드 혼합 타입은 바이너리로 처리
        if ("multipart".equals(mediaType.getType())) return true;
        if (mediaType.isCompatibleWith(MediaType.APPLICATION_JSON)) return false;
        if (mediaType.isCompatibleWith(MediaType.APPLICATION_XML)) return false;
        if (mediaType.isCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED)) return false;
        return true;
    }

    /**
     * Authorization 헤더 값을 {@value MASKED}로 마스킹한 새 헤더 객체를 반환한다.
     * 인증 정보가 로그에 노출되지 않도록 값을 대체한다.
     *
     * @param headers 원본 요청 헤더
     * @return Authorization 헤더가 마스킹된 헤더 복사본
     */
    HttpHeaders maskSensitiveHeaders(HttpHeaders headers) {
        HttpHeaders masked = new HttpHeaders();
        headers.forEach((name, values) -> {
            // Authorization 헤더는 인증 정보 노출 방지를 위해 값을 마스킹
            if (AUTHORIZATION.equalsIgnoreCase(name)) {
                masked.put(name, List.of(MASKED));
            } else {
                masked.put(name, values);
            }
        });
        return masked;
    }

    /**
     * 헤더 값이 단일 항목이면 문자열로, 복수이면 List 그대로 반환하여 JSON을 간결하게 만든다.
     *
     * @param headers 평탄화할 헤더
     * @return 헤더 이름 → 값(단일 문자열 또는 List) 맵
     */
    private Map<String, Object> flattenHeaders(HttpHeaders headers) {
        Map<String, Object> flat = new LinkedHashMap<>();
        headers.forEach((name, values) ->
                flat.put(name, values.size() == 1 ? values.get(0) : values));
        return flat;
    }

    /**
     * 응답 Body 스트림을 byte[]로 캐싱하여 여러 번 읽기 가능하게 하는 래퍼.
     * intercept() 내에서 로깅 후 원본 호출자에게 반환된다.
     */
    private static class BufferingClientHttpResponseWrapper implements ClientHttpResponse {

        private final ClientHttpResponse delegate;
        private final byte[] cachedBody;

        /**
         * 위임 응답의 Body를 즉시 읽어 캐싱한다.
         *
         * @param delegate 원본 HTTP 응답
         * @throws IOException Body 스트림 읽기 오류 시
         */
        BufferingClientHttpResponseWrapper(ClientHttpResponse delegate) throws IOException {
            this.delegate = delegate;
            // 생성 시점에 스트림을 전부 읽어 캐싱 (이후 getBody() 호출마다 새 스트림 반환)
            this.cachedBody = StreamUtils.copyToByteArray(delegate.getBody());
        }

        /**
         * 캐싱된 Body 바이트 배열을 반환한다 (로깅 내부 전용).
         *
         * @return 캐싱된 Body 바이트 배열
         */
        byte[] getBodyBytes() {
            return cachedBody;
        }

        /**
         * 캐싱된 Body를 새 ByteArrayInputStream으로 반환하여 재읽기를 허용한다.
         *
         * @return Body 스트림 (매 호출마다 처음부터 읽기 가능)
         */
        @Override
        public InputStream getBody() {
            return new ByteArrayInputStream(cachedBody);
        }

        @Override
        public HttpStatusCode getStatusCode() throws IOException {
            return delegate.getStatusCode();
        }

        @Override
        public String getStatusText() throws IOException {
            return delegate.getStatusText();
        }

        @Override
        public HttpHeaders getHeaders() {
            return delegate.getHeaders();
        }

        @Override
        public void close() {
            delegate.close();
        }
    }
}
