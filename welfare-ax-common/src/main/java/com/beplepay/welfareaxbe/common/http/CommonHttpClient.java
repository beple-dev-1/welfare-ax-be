package com.beplepay.welfareaxbe.common.http;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.beplepay.welfareaxbe.common.exception.ErrorCode;
import com.beplepay.welfareaxbe.common.exception.WelfareException;

/**
 * RestClient 기반 공통 HTTP 클라이언트.
 *
 * <p>GET·POST·PUT·DELETE 메서드를 지원하며 로깅·예외 처리·타임아웃·직렬화를 공통 처리한다.
 * <p>이 클래스는 HTTP 통신만 담당하며, 인증 방식을 알지 못한다.
 * Authorization·API Key·Basic Auth 등 인증 헤더는 이 클래스에서 생성하지 않는다.
 * 헤더는 호출부에서 {@code Map<String, String>}으로 조립하여 전달한다.
 *
 * <p>각 API 전용 설정(baseUrl, timeout)으로 인스턴스를 생성한 뒤 Spring Bean으로 등록하여 사용한다.
 */
public class CommonHttpClient {

    private final RestClient restClient;

    /**
     * 타임아웃 및 로깅 인터셉터가 구성된 CommonHttpClient를 생성한다.
     *
     * @param baseUrl        외부 API 기본 URL (예: https://httpbin.org)
     * @param connectTimeout 연결 타임아웃 (밀리초)
     * @param readTimeout    읽기 타임아웃 (밀리초)
     */
    public CommonHttpClient(String baseUrl, long connectTimeout, long readTimeout) {
        this(buildRestClient(baseUrl, connectTimeout, readTimeout));
    }

    /**
     * 테스트 전용 팩토리 메서드. MockRestServiceServer 기반 테스트에서 사용한다.
     * 프로덕션 코드에서는 {@link #CommonHttpClient(String, long, long)} 생성자를 사용한다.
     *
     * @param restClient MockRestServiceServer가 바인딩된 RestClient
     * @return CommonHttpClient 인스턴스
     */
    public static CommonHttpClient forTesting(RestClient restClient) {
        return new CommonHttpClient(restClient);
    }

    /** 내부 전용 생성자. */
    private CommonHttpClient(RestClient restClient) {
        this.restClient = restClient;
    }

    /**
     * 타임아웃·인터셉터가 적용된 RestClient를 생성한다.
     *
     * @param baseUrl        기본 URL
     * @param connectTimeout 연결 타임아웃 (밀리초)
     * @param readTimeout    읽기 타임아웃 (밀리초)
     * @return 구성된 RestClient
     */
    private static RestClient buildRestClient(String baseUrl, long connectTimeout, long readTimeout) {
        // Java 21 내장 HttpClient로 연결 타임아웃 설정
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectTimeout))
                .build();

        // 읽기 타임아웃은 JdkClientHttpRequestFactory에서 설정
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofMillis(readTimeout));

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .requestInterceptor(new HttpLoggingInterceptor())
                .build();
    }

    /**
     * GET 요청을 수행하고 응답을 역직렬화하여 반환한다.
     *
     * @param path         요청 경로 (baseUrl 기준 상대 경로)
     * @param headers      요청 헤더 (인증 헤더 포함 가능, 이 클래스에서 생성하지 않음)
     * @param responseType 응답 역직렬화 대상 타입
     * @param <T>          응답 타입
     * @return 역직렬화된 응답 객체
     * @throws WelfareException 외부 API 호출 실패 시 EXTERNAL_API_ERROR
     */
    public <T> T get(String path, Map<String, String> headers, Class<T> responseType) {
        try {
            return restClient.get()
                    .uri(path)
                    .headers(h -> headers.forEach(h::set))
                    .retrieve()
                    .body(responseType);
        } catch (RestClientException e) {
            throw toWelfareException(e);
        }
    }

    /**
     * GET 요청을 수행하고 제네릭 타입으로 응답을 역직렬화하여 반환한다.
     *
     * @param path         요청 경로
     * @param headers      요청 헤더
     * @param responseType 제네릭 응답 타입 참조 (예: {@code new ParameterizedTypeReference<Map<String, Object>>() {}})
     * @param <T>          응답 타입
     * @return 역직렬화된 응답 객체
     * @throws WelfareException 외부 API 호출 실패 시 EXTERNAL_API_ERROR
     */
    public <T> T get(String path, Map<String, String> headers, ParameterizedTypeReference<T> responseType) {
        try {
            return restClient.get()
                    .uri(path)
                    .headers(h -> headers.forEach(h::set))
                    .retrieve()
                    .body(responseType);
        } catch (RestClientException e) {
            throw toWelfareException(e);
        }
    }

    /**
     * POST 요청을 수행하고 제네릭 타입으로 응답을 역직렬화하여 반환한다.
     *
     * @param path         요청 경로
     * @param headers      요청 헤더
     * @param body         요청 본문 (Jackson으로 JSON 직렬화)
     * @param responseType 제네릭 응답 타입 참조
     * @param <T>          응답 타입
     * @return 역직렬화된 응답 객체
     * @throws WelfareException 외부 API 호출 실패 시 EXTERNAL_API_ERROR
     */
    public <T> T post(String path, Map<String, String> headers, Object body,
            ParameterizedTypeReference<T> responseType) {
        try {
            return restClient.post()
                    .uri(path)
                    .headers(h -> headers.forEach(h::set))
                    .body(body)
                    .retrieve()
                    .body(responseType);
        } catch (RestClientException e) {
            throw toWelfareException(e);
        }
    }

    /**
     * PUT 요청을 수행하고 제네릭 타입으로 응답을 역직렬화하여 반환한다.
     *
     * @param path         요청 경로
     * @param headers      요청 헤더
     * @param body         요청 본문 (Jackson으로 JSON 직렬화)
     * @param responseType 제네릭 응답 타입 참조
     * @param <T>          응답 타입
     * @return 역직렬화된 응답 객체
     * @throws WelfareException 외부 API 호출 실패 시 EXTERNAL_API_ERROR
     */
    public <T> T put(String path, Map<String, String> headers, Object body,
            ParameterizedTypeReference<T> responseType) {
        try {
            return restClient.put()
                    .uri(path)
                    .headers(h -> headers.forEach(h::set))
                    .body(body)
                    .retrieve()
                    .body(responseType);
        } catch (RestClientException e) {
            throw toWelfareException(e);
        }
    }

    /**
     * DELETE 요청을 수행한다.
     *
     * @param path    요청 경로
     * @param headers 요청 헤더
     * @throws WelfareException 외부 API 호출 실패 시 EXTERNAL_API_ERROR
     */
    public void delete(String path, Map<String, String> headers) {
        try {
            restClient.delete()
                    .uri(path)
                    .headers(h -> headers.forEach(h::set))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            throw toWelfareException(e);
        }
    }

    /**
     * RestClientException을 WelfareException(EXTERNAL_API_ERROR)으로 변환한다.
     * HTTP 4xx·5xx 오류 및 타임아웃·연결 실패를 단일 예외로 통일한다.
     *
     * @param e 발생한 RestClient 예외
     * @return WelfareException (EXTERNAL_API_ERROR)
     */
    private WelfareException toWelfareException(RestClientException e) {
        return new WelfareException(ErrorCode.EXTERNAL_API_ERROR, e.getMessage());
    }
}
