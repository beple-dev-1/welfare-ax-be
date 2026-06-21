package com.beplepay.welfareaxbe.user.client;

import java.util.Map;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

import com.beplepay.welfareaxbe.common.http.CommonHttpClient;

import lombok.RequiredArgsConstructor;

/**
 * httpbin.org를 대상으로 하는 테스트용 외부 API 클라이언트.
 *
 * <p>CommonHttpClient의 사용 패턴을 검증하며,
 * 실제 외부 API 클라이언트 구현의 참조 예시 역할을 한다.
 *
 * <p>인증이 필요 없는 httpbin.org 특성상 Content-Type 헤더만 전달한다.
 * 실제 API 클라이언트는 여기에 Authorization·API Key 등 헤더를 추가하여 주입한다.
 * CommonHttpClient는 헤더 내용을 알지 못하며, 전달받은 헤더를 그대로 요청에 포함한다.
 */
@Component
@RequiredArgsConstructor
public class TestApiClient {

    private final CommonHttpClient httpbinHttpClient;

    private static final Map<String, String> DEFAULT_HEADERS = Map.of(
            "Content-Type", "application/json"
    );

    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE =
            new ParameterizedTypeReference<>() {};

    /**
     * httpbin.org GET /get 엔드포인트를 호출한다.
     * 요청 정보(헤더, 파라미터 등)를 에코한 JSON을 반환한다.
     *
     * @return httpbin 응답 (요청 정보 에코)
     */
    public Map<String, Object> get() {
        return httpbinHttpClient.get("/get", DEFAULT_HEADERS, MAP_TYPE);
    }

    /**
     * httpbin.org POST /post 엔드포인트를 호출한다.
     * 요청 본문을 에코한 JSON을 반환한다.
     *
     * @param body 요청 본문
     * @return httpbin 응답 (요청 정보 에코)
     */
    public Map<String, Object> post(Map<String, Object> body) {
        return httpbinHttpClient.post("/post", DEFAULT_HEADERS, body, MAP_TYPE);
    }

    /**
     * httpbin.org PUT /put 엔드포인트를 호출한다.
     *
     * @param body 요청 본문
     * @return httpbin 응답
     */
    public Map<String, Object> put(Map<String, Object> body) {
        return httpbinHttpClient.put("/put", DEFAULT_HEADERS, body, MAP_TYPE);
    }

    /**
     * httpbin.org DELETE /delete 엔드포인트를 호출한다.
     */
    public void delete() {
        httpbinHttpClient.delete("/delete", DEFAULT_HEADERS);
    }
}
