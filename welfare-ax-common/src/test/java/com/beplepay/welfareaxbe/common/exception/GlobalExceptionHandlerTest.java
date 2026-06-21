package com.beplepay.welfareaxbe.common.exception;

import com.beplepay.welfareaxbe.common.response.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleValidationException_입력값검증실패_400응답() throws Exception {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "target");
        bindingResult.addError(new FieldError("target", "name", "이름은 필수입니다"));
        MethodArgumentNotValidException exception =
                new MethodArgumentNotValidException(null, bindingResult);

        ResponseEntity<ApiResponse<Void>> response = handler.handleValidationException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getCode()).isEqualTo("E001");
        assertThat(response.getBody().getMessage()).isEqualTo("이름은 필수입니다");
    }

    @Test
    void handleWelfareException_NotFound_404응답() {
        WelfareException exception = new WelfareException(ErrorCode.NOT_FOUND);

        ResponseEntity<ApiResponse<Void>> response = handler.handleWelfareException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getCode()).isEqualTo("E002");
        assertThat(response.getBody().getMessage()).isEqualTo("리소스를 찾을 수 없습니다");
    }

    @Test
    void handleWelfareException_E999직접메시지_500응답() {
        WelfareException exception = new WelfareException(ErrorCode.UNKNOWN, "처리 중 오류가 발생했습니다");

        ResponseEntity<ApiResponse<Void>> response = handler.handleWelfareException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getCode()).isEqualTo("E999");
        assertThat(response.getBody().getMessage()).isEqualTo("처리 중 오류가 발생했습니다");
    }

    @Test
    void handleException_미처리예외_500응답() {
        Exception exception = new RuntimeException("예상치 못한 오류");

        ResponseEntity<ApiResponse<Void>> response = handler.handleException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getCode()).isEqualTo("E999");
        assertThat(response.getBody().getMessage()).isEqualTo("기타 오류");
    }
}
