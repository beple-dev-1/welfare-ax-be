package com.beplepay.welfareaxbe.common.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;

import com.beplepay.welfareaxbe.common.util.MdcConstants;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * TraceIdFilter 단위 테스트.
 */
class TraceIdFilterTest {

    private TraceIdFilter filter;

    @BeforeEach
    void setUp() {
        filter = new TraceIdFilter();
        // 테스트 간 MDC 오염 방지
        MDC.remove(MdcConstants.TRACE_ID_KEY);
    }

    @Test
    void doFilterInternal_traceId_MDC저장및응답헤더설정() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        // 필터 체인 실행 중 MDC에 traceId가 설정되어 있는지 캡처
        String[] capturedTraceId = new String[1];
        FilterChain filterChain = (req, res) -> capturedTraceId[0] = MDC.get(MdcConstants.TRACE_ID_KEY);

        filter.doFilterInternal(request, response, filterChain);

        // 필터 체인 실행 중 MDC에 traceId가 존재해야 한다
        assertThat(capturedTraceId[0]).isNotNull().isNotEmpty();
        // 응답 헤더에 X-Trace-Id가 설정되어야 한다
        assertThat(response.getHeader(TraceIdFilter.TRACE_ID_HEADER)).isEqualTo(capturedTraceId[0]);
    }

    @Test
    void doFilterInternal_요청완료후_MDC제거() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        // 아무 작업 없이 통과하는 필터 체인
        FilterChain filterChain = (req, res) -> {};

        filter.doFilterInternal(request, response, filterChain);

        // 필터 체인 완료 후 MDC에 traceId가 제거되어야 한다
        assertThat(MDC.get(MdcConstants.TRACE_ID_KEY)).isNull();
    }

    @Test
    void doFilterInternal_예외발생시_MDC제거보장() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        // 필터 체인에서 RuntimeException 발생
        FilterChain filterChain = (req, res) -> { throw new RuntimeException("강제 예외 발생"); };

        assertThatThrownBy(() -> filter.doFilterInternal(request, response, filterChain))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("강제 예외 발생");

        // 예외 발생 후에도 finally 블록으로 MDC가 반드시 제거되어야 한다
        assertThat(MDC.get(MdcConstants.TRACE_ID_KEY)).isNull();
    }

    @Test
    void doFilterInternal_traceId_UUID형식() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        String[] capturedTraceId = new String[1];
        FilterChain filterChain = (req, res) -> capturedTraceId[0] = MDC.get(MdcConstants.TRACE_ID_KEY);

        filter.doFilterInternal(request, response, filterChain);

        // traceId는 UUID 형식(8-4-4-4-12 소문자 16진수)이어야 한다
        assertThat(capturedTraceId[0]).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }
}
