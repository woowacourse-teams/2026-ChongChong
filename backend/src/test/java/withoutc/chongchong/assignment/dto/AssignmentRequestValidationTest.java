package withoutc.chongchong.assignment.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import withoutc.chongchong.assignment.controller.dto.AssignmentCreateRequest;
import withoutc.chongchong.assignment.controller.dto.AssignmentUpdateRequest;

class AssignmentRequestValidationTest {

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
    @DisplayName("과제 생성 요청은 제목, 내용, 제출 방법의 최대 길이까지 허용한다")
    void validateCreateRequestAtLengthBoundaryTest() {
        AssignmentCreateRequest request = new AssignmentCreateRequest(
                "가".repeat(20),
                "가".repeat(10000),
                "가".repeat(10000),
                FUTURE,
                List.of(FUTURE)
        );

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    @DisplayName("과제 생성 요청의 제목, 내용, 제출 방법이 최대 길이를 초과하면 거부한다")
    void validateCreateRequestOverLengthTest() {
        AssignmentCreateRequest request = new AssignmentCreateRequest(
                "가".repeat(21),
                "가".repeat(10001),
                "가".repeat(10001),
                FUTURE,
                List.of(FUTURE)
        );

        assertThat(messages(validator.validate(request)))
                .containsExactlyInAnyOrder(
                        "제목은 20자 이내로 입력 가능합니다.",
                        "내용은 10,000자 이내로 입력 가능합니다.",
                        "제출 방법은 10,000자 이내로 입력 가능합니다."
                );
    }

    @Test
    @DisplayName("과제 생성 요청의 필수 필드가 누락되면 거부한다")
    void validateCreateRequestRequiredFieldsTest() {
        AssignmentCreateRequest request = new AssignmentCreateRequest(
                null,
                null,
                null,
                null,
                Collections.singletonList(null)
        );

        assertThat(validator.validate(request))
                .extracting(ConstraintViolation::getPropertyPath)
                .extracting(Object::toString)
                .containsExactlyInAnyOrder(
                        "title",
                        "content",
                        "submissionMethod",
                        "closeAt",
                        "remindAts[0].<list element>"
                );
        assertThat(messages(validator.validate(request)))
                .containsExactlyInAnyOrder(
                        "제목은 필수 값입니다.",
                        "내용은 필수 값입니다.",
                        "제출 방법은 필수 값입니다.",
                        "마감 시각은 필수 값입니다.",
                        "리마인드 시각은 필수 값입니다."
                );
    }

    @Test
    @DisplayName("과제 생성 요청의 마감 및 리마인드 시각은 미래여야 한다")
    void validateCreateRequestFutureTimesTest() {
        AssignmentCreateRequest request = new AssignmentCreateRequest(
                "과제 제목",
                "과제 내용",
                "링크 제출",
                PAST,
                List.of(PAST)
        );

        assertThat(messages(validator.validate(request)))
                .containsExactlyInAnyOrder(
                        "마감 시각은 현재보다 미래여야 합니다.",
                        "리마인드 시각은 현재보다 미래여야 합니다."
                );
    }

    @Test
    @DisplayName("과제 수정 요청은 모든 필드를 생략할 수 있다")
    void validateEmptyUpdateRequestTest() {
        AssignmentUpdateRequest request = new AssignmentUpdateRequest(null, null, null, null, null);

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    @DisplayName("과제 수정 요청의 제목, 내용, 제출 방법이 최대 길이를 초과하면 거부한다")
    void validateUpdateRequestOverLengthTest() {
        AssignmentUpdateRequest request = new AssignmentUpdateRequest(
                "가".repeat(21),
                "가".repeat(10001),
                "가".repeat(10001),
                null,
                null
        );

        assertThat(messages(validator.validate(request)))
                .containsExactlyInAnyOrder(
                        "제목은 20자 이내로 입력 가능합니다.",
                        "내용은 10,000자 이내로 입력 가능합니다.",
                        "제출 방법은 10,000자 이내로 입력 가능합니다."
                );
    }

    @Test
    @DisplayName("과제 수정 요청에 시각을 제공하면 마감 및 리마인드 시각은 미래여야 한다")
    void validateUpdateRequestFutureTimesTest() {
        AssignmentUpdateRequest request = new AssignmentUpdateRequest(
                null,
                null,
                null,
                PAST,
                List.of(PAST)
        );

        assertThat(messages(validator.validate(request)))
                .containsExactlyInAnyOrder(
                        "마감 시각은 현재보다 미래여야 합니다.",
                        "리마인드 시각은 현재보다 미래여야 합니다."
                );
    }

    private Set<String> messages(Set<? extends ConstraintViolation<?>> violations) {
        return violations.stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());
    }
}
