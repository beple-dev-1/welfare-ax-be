package com.beplepay.weadk.welfare.common.response;

import com.beplepay.weadk.welfare.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {

    @Test
    void success_데이터있음_정상응답() {
        ApiResponse<String> response = ApiResponse.success("test-data");

        assertThat(response.getCode()).isEqualTo("0000");
        assertThat(response.getMessage()).isEqualTo("성공");
        assertThat(response.getData()).isEqualTo("test-data");
    }

    @Test
    void success_데이터없음_정상응답() {
        ApiResponse<Void> response = ApiResponse.success();

        assertThat(response.getCode()).isEqualTo("0000");
        assertThat(response.getMessage()).isEqualTo("성공");
        assertThat(response.getData()).isNull();
    }

    @Test
    void error_정의된에러코드_기본메시지반환() {
        ApiResponse<Void> response = ApiResponse.error(ErrorCode.NOT_FOUND);

        assertThat(response.getCode()).isEqualTo("E002");
        assertThat(response.getMessage()).isEqualTo("리소스를 찾을 수 없습니다");
        assertThat(response.getData()).isNull();
    }

    @Test
    void error_E999_직접메시지오버라이드() {
        ApiResponse<Void> response = ApiResponse.error(ErrorCode.UNKNOWN, "주문번호가 없습니다");

        assertThat(response.getCode()).isEqualTo("E999");
        assertThat(response.getMessage()).isEqualTo("주문번호가 없습니다");
    }
}
