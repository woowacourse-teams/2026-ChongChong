package withoutc.chongchong.assignment.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import jakarta.validation.Validator;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import withoutc.chongchong.assignment.controller.dto.AssignmentCreateRequest;
import withoutc.chongchong.assignment.entity.Assignment;
import withoutc.chongchong.assignment.exception.AssignmentErrorCode;
import withoutc.chongchong.assignment.exception.AssignmentException;
import withoutc.chongchong.study.entity.Study;

@ActiveProfiles("test")
@Import(AssignmentValidationClockTest.FixedClockConfig.class)
@SpringBootTest
class AssignmentValidationClockTest {

    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Seoul");
    private static final Instant INSTANT = Instant.parse("2026-08-20T01:00:00Z");
    private static final LocalDateTime NOW = LocalDateTime.ofInstant(INSTANT, ZONE_ID);

    @Autowired
    private Validator validator;

    @Test
    @DisplayName("현재 시각은 Bean Validation과 과제 도메인에서 모두 거부한다")
    void rejectCurrentTimeTest() {
        AssignmentCreateRequest closeAtRequest = createRequest(NOW, NOW.plusNanos(1));
        AssignmentCreateRequest remindAtRequest = createRequest(NOW.plusNanos(1), NOW);
        Assignment assignment = createAssignment(NOW.plusNanos(1));

        assertThat(validator.validate(closeAtRequest))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("closeAt");
        assertThat(validator.validate(remindAtRequest))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("remindAts[0].<list element>");

        assertThatThrownBy(() -> Assignment.create(
                mock(Study.class),
                "과제 제목",
                "과제 내용",
                "링크 제출",
                NOW,
                NOW
        ))
                .isInstanceOf(AssignmentException.class)
                .extracting(exception -> ((AssignmentException) exception).getErrorCode())
                .isEqualTo(AssignmentErrorCode.INVALID_CLOSE_AT);
        assertThatThrownBy(() -> assignment.addReminders(List.of(NOW), NOW))
                .isInstanceOf(AssignmentException.class)
                .extracting(exception -> ((AssignmentException) exception).getErrorCode())
                .isEqualTo(AssignmentErrorCode.INVALID_REMIND_AT);
    }

    @Test
    @DisplayName("현재보다 1나노초 미래인 시각은 Bean Validation과 과제 도메인에서 모두 허용한다")
    void acceptFutureTimeTest() {
        LocalDateTime future = NOW.plusNanos(1);
        AssignmentCreateRequest request = createRequest(future, future);
        Assignment assignment = createAssignment(future);

        assertThat(validator.validate(request)).isEmpty();
        assignment.addReminders(List.of(future), NOW);

        assertThat(assignment.getCloseAt()).isEqualTo(future);
        assertThat(assignment.getNextRemindAt()).isEqualTo(future);
    }

    private AssignmentCreateRequest createRequest(LocalDateTime closeAt, LocalDateTime remindAt) {
        return new AssignmentCreateRequest(
                "과제 제목",
                "과제 내용",
                "링크 제출",
                closeAt,
                List.of(remindAt)
        );
    }

    private Assignment createAssignment(LocalDateTime closeAt) {
        return Assignment.create(
                mock(Study.class),
                "과제 제목",
                "과제 내용",
                "링크 제출",
                closeAt,
                NOW
        );
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FixedClockConfig {

        @Primary
        @Bean
        Clock fixedClock() {
            return Clock.fixed(INSTANT, ZONE_ID);
        }
    }
}
