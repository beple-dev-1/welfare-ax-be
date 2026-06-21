package com.beplepay.welfareaxbe.user.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class JwtProviderTest {

    private JwtProvider jwtProvider;

    private static final String TEST_SECRET =
            "test-secret-key-for-welfare-ax-be-unit-test-at-least-32-chars";
    private static final long EXPIRATION = 43200L;

    @BeforeEach
    void setUp() {
        jwtProvider = new JwtProvider();
        ReflectionTestUtils.setField(jwtProvider, "secret", TEST_SECRET);
        ReflectionTestUtils.setField(jwtProvider, "expirationSeconds", EXPIRATION);
    }

    @Test
    void generateToken_JWT형식_문자열반환() {
        String token = jwtProvider.generateToken(1L, "USER");

        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    void validateToken_유효한토큰_true반환() {
        String token = jwtProvider.generateToken(1L, "USER");

        assertThat(jwtProvider.validateToken(token)).isTrue();
    }

    @Test
    void validateToken_만료된토큰_false반환() {
        ReflectionTestUtils.setField(jwtProvider, "expirationSeconds", 0L);
        String token = jwtProvider.generateToken(1L, "USER");

        assertThat(jwtProvider.validateToken(token)).isFalse();
    }

    @Test
    void validateToken_잘못된서명_false반환() {
        JwtProvider otherProvider = new JwtProvider();
        ReflectionTestUtils.setField(otherProvider, "secret",
                "other-secret-key-for-welfare-ax-be-unit-test-at-least-32");
        ReflectionTestUtils.setField(otherProvider, "expirationSeconds", EXPIRATION);
        String token = otherProvider.generateToken(1L, "USER");

        assertThat(jwtProvider.validateToken(token)).isFalse();
    }

    @Test
    void getMemberId_Claims파싱_올바른memberId() {
        String token = jwtProvider.generateToken(42L, "USER");

        assertThat(jwtProvider.getMemberId(token)).isEqualTo(42L);
    }

    @Test
    void getRole_Claims파싱_올바른role() {
        String token = jwtProvider.generateToken(1L, "USER");

        assertThat(jwtProvider.getRole(token)).isEqualTo("USER");
    }
}
