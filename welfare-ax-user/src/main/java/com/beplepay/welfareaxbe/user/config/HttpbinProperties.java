package com.beplepay.welfareaxbe.user.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

/**
 * application-api.yaml의 {@code api.httpbin} 설정을 바인딩하는 프로퍼티 클래스.
 *
 * <p>httpbin.org 연결에 필요한 URL과 타임아웃 설정을 관리한다.
 * 운영 환경에서는 환경변수(예: {@code API_HTTPBIN_BASE_URL})로 오버라이드한다.
 */
@ConfigurationProperties(prefix = "api.httpbin")
@Getter
@Setter
public class HttpbinProperties {

    /** httpbin.org 기본 URL */
    private String baseUrl;

    /** 연결 타임아웃 (밀리초) */
    private long connectTimeout;

    /** 읽기 타임아웃 (밀리초) */
    private long readTimeout;
}
