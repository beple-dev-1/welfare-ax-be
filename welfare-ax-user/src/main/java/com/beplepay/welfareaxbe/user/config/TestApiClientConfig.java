package com.beplepay.welfareaxbe.user.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.beplepay.welfareaxbe.common.http.CommonHttpClient;

/**
 * TestApiClient에서 사용할 CommonHttpClient 빈을 등록하는 설정 클래스.
 *
 * <p>application-api.yaml의 {@code api.httpbin} 설정을 읽어 타임아웃을 구성한다.
 * 각 외부 API 클라이언트는 이 패턴과 같이 전용 CommonHttpClient 빈을 등록하여 사용한다.
 * <p>{@code api.httpbin.base-url}이 설정된 경우에만 빈을 등록한다 (api 프로파일 미활성화 시 스킵).
 */
@Configuration
@ConditionalOnProperty(prefix = "api.httpbin", name = "base-url")
@EnableConfigurationProperties(HttpbinProperties.class)
public class TestApiClientConfig {

    /**
     * httpbin.org 전용 CommonHttpClient 빈을 생성한다.
     *
     * @param properties application-api.yaml에서 바인딩된 httpbin 연결 설정
     * @return httpbin.org 전용 CommonHttpClient 인스턴스
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
