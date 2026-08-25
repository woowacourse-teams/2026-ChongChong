package withoutc.chongchong.notice.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import withoutc.chongchong.notice.controller.dto.NoticeCreateRequest;
import withoutc.chongchong.notice.controller.dto.NoticeUpdateRequest;

class NoticeRequestValidationTest {

    private static final LocalDateTime FUTURE = LocalDateTime.of(2099, 1, 1, 0, 0);
    private static final LocalDateTime PAST = LocalDateTime.of(2000, 1, 1, 0, 0);

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidatorFactory() {
        validatorFactory.close();
    }

    @Test
    @DisplayName("공지 생성 요청은 제목 15자와 내용 10000자까지 허용한다")
    void validateCreateRequestAtLengthBoundaryTest() {
        NoticeCreateRequest request = new NoticeCreateRequest(
                "가".repeat(15),
                "가".repeat(10000),
                List.of(FUTURE)
        );

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    @DisplayName("공지 생성 요청의 제목과 내용이 최대 길이를 초과하면 거부한다")
    void validateCreateRequestOverLengthTest() {
        NoticeCreateRequest request = new NoticeCreateRequest(
                "가".repeat(16),
                "가".repeat(10001),
                List.of(FUTURE)
        );

        assertThat(messages(validator.validate(request)))
                .containsExactlyInAnyOrder(
                        "제목은 15자 이내로 입력 가능합니다.",
                        "내용은 10,000자 이내로 입력 가능합니다."
                );
    }

    @Test
    @DisplayName("공지 생성 요청의 리마인드 시각은 null이거나 과거일 수 없다")
    void validateCreateRequestRemindAtTest() {
        NoticeCreateRequest nullRemindAtRequest = new NoticeCreateRequest(
                "공지 제목",
                "공지 내용",
                Collections.singletonList(null)
        );
        NoticeCreateRequest pastRemindAtRequest = new NoticeCreateRequest(
                "공지 제목",
                "공지 내용",
                List.of(PAST)
        );

        assertThat(messages(validator.validate(nullRemindAtRequest)))
                .containsExactly("리마인드 시각은 필수 값입니다.");
        assertThat(messages(validator.validate(pastRemindAtRequest)))
                .containsExactly("리마인드 시각은 현재보다 미래여야 합니다.");
    }

    @Test
    @DisplayName("공지 수정 요청은 모든 필드를 생략할 수 있다")
    void validateEmptyUpdateRequestTest() {
        NoticeUpdateRequest request = new NoticeUpdateRequest(null, null, null);

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    @DisplayName("공지 수정 요청의 제목과 내용이 최대 길이를 초과하면 거부한다")
    void validateUpdateRequestOverLengthTest() {
        NoticeUpdateRequest request = new NoticeUpdateRequest(
                "가".repeat(16),
                "가".repeat(10001),
                null
        );

        assertThat(messages(validator.validate(request)))
                .containsExactlyInAnyOrder(
                        "제목은 15자 이내로 입력 가능합니다.",
                        "내용은 10,000자 이내로 입력 가능합니다."
                );
    }

    @Test
    @DisplayName("공지 수정 요청의 리마인드 시각은 null이거나 과거일 수 없다")
    void validateUpdateRequestRemindAtTest() {
        NoticeUpdateRequest nullRemindAtRequest = new NoticeUpdateRequest(
                null,
                null,
                Collections.singletonList(null)
        );
        NoticeUpdateRequest pastRemindAtRequest = new NoticeUpdateRequest(
                null,
                null,
                List.of(PAST)
        );

        assertThat(messages(validator.validate(nullRemindAtRequest)))
                .containsExactly("리마인드 시각은 null일 수 없습니다.");
        assertThat(messages(validator.validate(pastRemindAtRequest)))
                .containsExactly("리마인드 시각은 미래여야 합니다.");
    }

    private Set<String> messages(Set<? extends ConstraintViolation<?>> violations) {
        return violations.stream()
                .map(ConstraintViolation::getMessage)
                .collect(java.util.stream.Collectors.toSet());
    }
}
