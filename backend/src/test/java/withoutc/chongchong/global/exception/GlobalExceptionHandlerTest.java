package withoutc.chongchong.global.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import withoutc.chongchong.global.exception.code.ErrorCode;
import withoutc.chongchong.global.exception.handler.GlobalExceptionHandler;

@WebMvcTest(controllers = GlobalExceptionHandlerTest.TestController.class)
@Import({GlobalExceptionHandler.class, GlobalExceptionHandlerTest.TestController.class})
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("DTO 필드 검증 실패 시 필드별 검증 사유를 반환한다")
    void handleBindException() throws Exception {
        mockMvc.perform(post("/test/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
                .andExpect(jsonPath("$.message").value("입력값이 올바르지 않습니다."))
                .andExpect(jsonPath("$.errors[0].field").value("name"))
                .andExpect(jsonPath("$.errors[0].reason").value("이름은 필수입니다."));
    }

    @Test
    @DisplayName("메서드 파라미터 검증 실패 시 공통 입력값 오류를 반환한다")
    void handleMethodValidationException() throws Exception {
        mockMvc.perform(get("/test/parameter-validation")
                        .param("page", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT_VALUE"))
                .andExpect(jsonPath("$.message").value("입력값이 올바르지 않습니다."))
                .andExpect(jsonPath("$.errors").doesNotExist());
    }

    @Test
    @DisplayName("반환값 검증 실패 시 내부 서버 오류를 반환한다")
    void handleReturnValueValidationException() throws Exception {
        mockMvc.perform(get("/test/return-validation"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.message").value("서버 내부 오류가 발생했습니다."))
                .andExpect(jsonPath("$.errors").doesNotExist());
    }

    @Test
    @DisplayName("읽을 수 없는 JSON 요청은 잘못된 요청으로 반환한다")
    void handleHttpMessageNotReadableException() throws Exception {
        mockMvc.perform(post("/test/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("요청 형식이 잘못되었습니다."))
                .andExpect(jsonPath("$.errors").doesNotExist());
    }

    @Test
    @DisplayName("요청 파라미터 타입이 올바르지 않으면 잘못된 요청으로 반환한다")
    void handleMethodArgumentTypeMismatchException() throws Exception {
        mockMvc.perform(get("/test/parameter-validation")
                        .param("page", "not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("요청 형식이 잘못되었습니다."));
    }

    @Test
    @DisplayName("필수 요청 파라미터가 없으면 잘못된 요청으로 반환한다")
    void handleServletRequestBindingException() throws Exception {
        mockMvc.perform(get("/test/parameter-validation"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("요청 형식이 잘못되었습니다."));
    }

    @Test
    @DisplayName("지원하지 않는 HTTP 메서드는 405 응답으로 반환한다")
    void handleUnsupportedMethodException() throws Exception {
        mockMvc.perform(post("/test/method"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_HTTP_METHOD"))
                .andExpect(jsonPath("$.message").value("지원하지 않는 HTTP 메서드입니다."));
    }

    @Test
    @DisplayName("존재하지 않는 경로는 404 응답으로 반환한다")
    void handleUnsupportedPathException() throws Exception {
        mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_PATH"))
                .andExpect(jsonPath("$.message").value("존재하지 않는 경로입니다."));
    }

    @Test
    @DisplayName("비즈니스 예외는 에러 코드에 정의된 응답으로 반환한다")
    void handleBusinessException() throws Exception {
        mockMvc.perform(get("/test/business-exception"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TEST_BUSINESS_ERROR"))
                .andExpect(jsonPath("$.message").value("테스트 비즈니스 예외입니다."))
                .andExpect(jsonPath("$.errors").doesNotExist());
    }

    @Test
    @DisplayName("처리하지 못한 예외는 내부 정보를 노출하지 않고 500 응답으로 반환한다")
    void handleException() throws Exception {
        mockMvc.perform(get("/test/unexpected-exception"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.message").value("서버 내부 오류가 발생했습니다."))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.not("노출되면 안 되는 메시지")))
                .andExpect(jsonPath("$.errors").doesNotExist());
    }

    @RestController
    @RequestMapping("/test")
    static class TestController {

        @PostMapping("/validation")
        void validateRequest(@Valid @RequestBody TestRequest request) {
        }

        @GetMapping("/parameter-validation")
        void validateParameter(
                @RequestParam @Positive(message = "페이지는 양수여야 합니다.") int page
        ) {
        }

        @GetMapping("/return-validation")
        @NotNull(message = "응답은 null일 수 없습니다.")
        String validateReturnValue() {
            return null;
        }

        @GetMapping("/method")
        void method() {
        }

        @GetMapping("/business-exception")
        void businessException() {
            throw new TestBusinessException();
        }

        @GetMapping("/unexpected-exception")
        void unexpectedException() {
            throw new IllegalStateException("노출되면 안 되는 메시지");
        }
    }

    private record TestRequest(
            @NotBlank(message = "이름은 필수입니다.")
            String name
    ) {
    }

    private enum TestErrorCode implements ErrorCode {

        TEST_BUSINESS_ERROR;

        @Override
        public HttpStatus getHttpStatus() {
            return HttpStatus.CONFLICT;
        }

        @Override
        public String getCode() {
            return name();
        }

        @Override
        public String getMessage() {
            return "테스트 비즈니스 예외입니다.";
        }
    }

    private static class TestBusinessException extends BusinessException {

        TestBusinessException() {
            super(TestErrorCode.TEST_BUSINESS_ERROR);
        }
    }
}
