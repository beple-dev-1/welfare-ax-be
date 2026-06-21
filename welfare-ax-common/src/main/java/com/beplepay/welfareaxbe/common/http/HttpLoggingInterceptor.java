package com.beplepay.welfareaxbe.common.http;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

/**
 * RestClient 요청·응답 공통 로깅 인터셉터.
 *
 * <p>요청: HTTP 메서드, URI, 헤더 (Authorization 헤더 값은 마스킹)
 * <p>응답: HTTP 상태 코드
 * <p>응답 Body는 스트림 소비 문제를 방지하기 위해 로깅하지 않는다.
 */
public class HttpLoggingInterceptor implements ClientHttpRequestInterceptor {

    private static final Logger log = LoggerFactory.getLogger(HttpLoggingInterceptor.class);
    private static final String MASKED = "****";
    private static final String AUTHORIZATION = "Authorization";

    /**
     * 요청 전후로 로깅을 수행하고 실제 요청을 실행한다.
     *
     * @param request   HTTP 요청 정보
     * @param body      요청 바이트 배열
     * @param execution 실제 요청 실행기
     * @return HTTP 응답
     * @throws IOException 요청 실행 중 I/O 오류 발생 시
     */
    @Override
    public ClientHttpResponse intercept(
            HttpRequest request, byte[] body, ClientHttpRequestExecution execution
    ) throws IOException {
        logRequest(request);
        ClientHttpResponse response = execution.execute(request, body);
        logResponse(response);
        return response;
    }

    /**
     * 요청 메서드, URI, 헤더를 DEBUG 레벨로 기록한다.
     *
     * @param request HTTP 요청 정보
     */
    private void logRequest(HttpRequest request) {
        log.debug("[HTTP 요청] {} {} | 헤더: {}",
                request.getMethod(),
                request.getURI(),
                maskSensitiveHeaders(request.getHeaders()));
    }

    /**
     * 응답 상태 코드를 DEBUG 레벨로 기록한다.
     *
     * @param response HTTP 응답
     * @throws IOException 상태 코드 조회 중 I/O 오류 발생 시
     */
    private void logResponse(ClientHttpResponse response) throws IOException {
        log.debug("[HTTP 응답] 상태: {}", response.getStatusCode());
    }

    /**
     * Authorization 헤더 값을 마스킹한 새 헤더 객체를 반환한다.
     * 인증 정보가 로그에 노출되지 않도록 값을 {@value MASKED}로 대체한다.
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
}
