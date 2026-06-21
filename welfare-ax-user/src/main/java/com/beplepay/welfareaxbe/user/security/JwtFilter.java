package com.beplepay.welfareaxbe.user.security;

import java.io.IOException;
import java.util.List;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import lombok.RequiredArgsConstructor;

/**
 * HTTP 요청에서 JWT 토큰을 추출하고 SecurityContext에 인증 정보를 설정하는 필터.
 * 요청당 한 번만 실행되며, 유효하지 않은 토큰은 SecurityContext를 설정하지 않고 다음 필터로 넘긴다.
 */
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;

    /**
     * Authorization 헤더의 JWT 토큰을 검증하고 SecurityContext에 인증 정보를 등록한다.
     *
     * @param request     HTTP 요청
     * @param response    HTTP 응답
     * @param filterChain 다음 필터 체인
     * @throws ServletException 서블릿 처리 오류
     * @throws IOException      I/O 오류
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // Authorization 헤더에서 Bearer 토큰 추출
        String token = extractToken(request);

        // 토큰이 존재하고 유효한 경우에만 SecurityContext에 인증 정보 설정
        if (StringUtils.hasText(token) && jwtProvider.validateToken(token)) {
            Long memberId = jwtProvider.getMemberId(token);
            String role = jwtProvider.getRole(token);

            // Spring Security 인증 객체 생성: principal=memberId, credentials=null
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            memberId,
                            null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + role))
                    );
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        // 인증 성공 여부와 관계없이 다음 필터로 진행 (인가는 SecurityConfig에서 처리)
        filterChain.doFilter(request, response);
    }

    /**
     * HTTP 요청의 Authorization 헤더에서 토큰 문자열을 추출한다.
     *
     * @param request HTTP 요청
     * @return Bearer 접두사를 제거한 순수 토큰 문자열, 헤더가 없거나 형식이 다르면 null
     */
    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        // "Bearer " 접두사 확인 후 순수 토큰 추출 (7자 이후)
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        return null;
    }
}
