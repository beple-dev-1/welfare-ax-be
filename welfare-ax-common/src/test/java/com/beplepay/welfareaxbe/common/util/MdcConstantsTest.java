package com.beplepay.welfareaxbe.common.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MdcConstants 단위 테스트.
 */
class MdcConstantsTest {

    @Test
    void traceIdKey_값이_traceId() {
        // MdcConstants.TRACE_ID_KEY는 logback-spring.xml의 %X{traceId:-NO_TRACE} 패턴과 일치해야 한다
        assertThat(MdcConstants.TRACE_ID_KEY).isEqualTo("traceId");
    }
}
