package com.beplepay.welfareaxbe.common.util;

/**
 * MDC(Mapped Diagnostic Context) 키 상수 클래스.
 *
 * <p>logback-spring.xml의 {@code %X{traceId:-NO_TRACE}} 패턴과
 * {@link com.beplepay.welfareaxbe.common.filter.TraceIdFilter},
 * {@link com.beplepay.welfareaxbe.common.http.HttpLoggingInterceptor}에서
 * 동일한 키를 사용하도록 단일 상수로 관리한다.
 *
 * <p>logback XML은 Java 상수를 직접 참조할 수 없으므로, 이 클래스의 상수값을
 * logback 패턴에 직접 기입해야 한다. 키를 변경할 경우 logback-spring.xml의
 * {@code %X{traceId:-NO_TRACE}} 패턴도 함께 수정해야 한다.
 */
public final class MdcConstants {

    /**
     * TraceIdFilter가 MDC에 저장하는 traceId 키.
     * logback 패턴 {@code %X{traceId:-NO_TRACE}}와 값이 일치해야 한다.
     */
    public static final String TRACE_ID_KEY = "traceId";

    private MdcConstants() {}
}
