package com.beplepay.weadk.welfare.user.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

/**
 * JWT 액세스 토큰 생성·검증·파싱을 담당하는 컴포넌트.
 * 시크릿 키와 만료 시간은 실행 환경별 application.yaml에서 주입받는다.
 */
@Slf4j
@Component
public class JwtProvider {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expirationSeconds;

    /**
     * HMAC-SHA 서명에 사용할 SecretKey를 생성한다.
     *
     * @return HMAC-SHA 서명 키
     */
    private SecretKey getSigningKey() {
        // UTF-8로 인코딩된 시크릿 문자열을 HMAC-SHA 키로 변환
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 회원 ID와 권한으로 JWT 액세스 토큰을 생성한다.
     *
     * @param memberId 회원 식별자 (subject 클레임에 저장)
     * @param role     권한 문자열 (예: USER, ADMIN)
     * @return 서명된 JWT 문자열
     */
    public String generateToken(Long memberId, String role) {
        Date now = new Date();
        // 현재 시각 기준으로 만료 시각 계산 (초 단위 설정값 → 밀리초 변환)
        Date expiry = new Date(now.getTime() + expirationSeconds * 1000);

        return Jwts.builder()
                .subject(String.valueOf(memberId))
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * JWT 토큰의 서명·만료 유효성을 검사한다.
     *
     * @param token 검사할 JWT 문자열
     * @return 유효하면 true, 만료·변조·형식 오류이면 false
     */
    public boolean validateToken(String token) {
        try {
            // 서명 검증 및 만료 시간 확인
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            // 만료·변조·형식 오류 모두 false로 통일 처리 (상세 원인은 로그로만 기록)
            log.debug("유효하지 않은 JWT 토큰: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 토큰에서 회원 ID를 추출한다.
     *
     * @param token 유효한 JWT 문자열
     * @return 회원 식별자
     */
    public Long getMemberId(String token) {
        // subject 클레임에 저장된 회원 ID를 Long으로 변환
        return Long.valueOf(parseClaims(token).getSubject());
    }

    /**
     * 토큰에서 권한 문자열을 추출한다.
     *
     * @param token 유효한 JWT 문자열
     * @return 권한 문자열 (예: USER, ADMIN)
     */
    public String getRole(String token) {
        return parseClaims(token).get("role", String.class);
    }

    /**
     * 토큰을 파싱하여 클레임 페이로드를 반환한다.
     *
     * @param token 파싱할 JWT 문자열
     * @return 클레임 페이로드
     * @throws JwtException 서명 검증 실패 또는 만료된 경우
     */
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
