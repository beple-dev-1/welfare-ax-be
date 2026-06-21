package com.beplepay.welfareaxbe.common.filter;

import java.io.IOException;
import java.util.UUID;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.beplepay.welfareaxbe.common.util.MdcConstants;

/**
 * 모든 인바운드 HTTP 요청에 UUID 기반 traceId를 부여하는 서블릿 필터.
 *
 * <p>동작 순서:
 * <ol>
 *   <li>UUID를 생성하여 MDC {@code MdcConstants#TRACE_ID_KEY} 키에 저장한다.
 *   <li>응답 헤더 {@value TRACE_ID_HEADER}에 traceId를 설정하여 클라이언트에 반환한다.
 *   <li>필터 체인 실행 후 finally 블록에서 MDC를 제거하여 스레드 재사용 시 누수를 방지한다.
 * </ol>
 *
 * <p>{@code @Order(Ordered.HIGHEST_PRECEDENCE)}로 등록되어 Security 필터 체인보다
 * 먼저 실행된다. 클라이언트가 전송한 X-Trace-Id 헤더는 무시하고 서버에서 신규 생성한다.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    static final String TRACE_ID_HEADER = "X-Trace-Id";

    /**
     * 요청 진입 시 traceId를 생성하여 MDC와 응답 헤더에 설정하고,
     * 요청 처리 완료 후 MDC에서 제거한다.
     *
     * @param request     HTTP 서블릿 요청
     * @param response    HTTP 서블릿 응답
     * @param filterChain 다음 필터 체인
     * @throws ServletException 필터 처리 중 서블릿 오류 발생 시
     * @throws IOException      필터 처리 중 I/O 오류 발생 시
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String traceId = UUID.randomUUID().toString();
        MDC.put(MdcConstants.TRACE_ID_KEY, traceId);
        // 응답 헤더에 traceId를 설정하여 클라이언트가 요청 흐름을 추적 가능하게 한다
        response.setHeader(TRACE_ID_HEADER, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            // 스레드풀 재사용 시 이전 traceId가 다음 요청에 누수되지 않도록 제거
            // MDC.clear() 대신 remove()를 사용하여 다른 MDC 항목은 보존한다
            MDC.remove(MdcConstants.TRACE_ID_KEY);
        }
    }
}
