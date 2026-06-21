package com.beplepay.welfareaxbe.user.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.beplepay.welfareaxbe.common.exception.ErrorCode;
import com.beplepay.welfareaxbe.user.security.JwtFilter;
import com.beplepay.welfareaxbe.user.security.JwtProvider;
import lombok.RequiredArgsConstructor;

/**
 * Spring Security 설정 클래스.
 * JWT 기반 무상태(Stateless) 인증을 구성하고, 공개·보호 API 접근 권한을 정의한다.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtProvider jwtProvider;

    /**
     * SecurityFilterChain을 구성한다.
     * CSRF·HTTP Basic·폼 로그인 비활성화, 세션 STATELESS 정책을 적용하며
     * 인증·권한 오류 시 JSON 응답을 반환한다.
     *
     * @param http HttpSecurity 빌더
     * @return 구성된 SecurityFilterChain
     * @throws Exception 설정 오류
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                // JWT 사용으로 CSRF·세션·폼 로그인 불필요
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 인증 없이 접근 허용: 로그인·회원가입, 헬스체크
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        // Swagger UI 접근 허용 — 운영 환경은 springdoc.swagger-ui.enabled=false로 이중 차단
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(e -> e
                        // 인증되지 않은 요청 → 401 Unauthorized
                        .authenticationEntryPoint((request, response, ex) ->
                                writeErrorResponse(response, HttpServletResponse.SC_UNAUTHORIZED, ErrorCode.UNAUTHORIZED)
                        )
                        // 권한이 없는 요청 → 403 Forbidden
                        .accessDeniedHandler((request, response, ex) ->
                                writeErrorResponse(response, HttpServletResponse.SC_FORBIDDEN, ErrorCode.FORBIDDEN)
                        )
                )
                .addFilterBefore(new JwtFilter(jwtProvider), UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /**
     * 비밀번호 암호화에 사용할 PasswordEncoder Bean을 등록한다.
     *
     * @return BCrypt 알고리즘 기반 PasswordEncoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 인증·권한 오류 발생 시 JSON 형식의 오류 응답을 작성한다.
     * ObjectMapper Bean 의존성 없이 String.format으로 직접 JSON을 구성한다.
     *
     * @param response  HTTP 응답 객체
     * @param status    HTTP 상태 코드
     * @param errorCode 응답에 사용할 오류 코드
     * @throws IOException 응답 쓰기 오류
     */
    private void writeErrorResponse(HttpServletResponse response, int status, ErrorCode errorCode) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        // JSON 구조: {"code":"Exxx","message":"..."}
        response.getWriter().write(String.format(
                "{\"code\":\"%s\",\"message\":\"%s\"}",
                errorCode.getCode(),
                errorCode.getDefaultMessage()
        ));
    }
}
