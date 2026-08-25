package withoutc.chongchong.notice.dto;

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
import withoutc.chongchong.notice.controller.dto.NoticeCreateRequest;
import withoutc.chongchong.notice.entity.Notice;
import withoutc.chongchong.notice.exception.NoticeErrorCode;
import withoutc.chongchong.notice.exception.NoticeException;
import withoutc.chongchong.study.entity.StudyMember;

@ActiveProfiles("test")
@Import(NoticeValidationClockTest.FixedClockConfig.class)
@SpringBootTest
class NoticeValidationClockTest {

    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Seoul");
    private static final Instant INSTANT = Instant.parse("2026-08-20T01:00:00Z");
    private static final LocalDateTime NOW = LocalDateTime.ofInstant(INSTANT, ZONE_ID);

    @Autowired
    private Validator validator;

    @Test
    @DisplayName("현재 시각은 Bean Validation과 공지 도메인에서 모두 거부한다")
    void rejectCurrentTimeTest() {
        NoticeCreateRequest request = new NoticeCreateRequest("공지 제목", "공지 내용", List.of(NOW));
        Notice notice = createNotice();

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("remindAts[0].<list element>");
        assertThatThrownBy(() -> notice.addReminders(request.remindAts(), NOW))
                .isInstanceOf(NoticeException.class)
                .extracting(exception -> ((NoticeException) exception).getErrorCode())
                .isEqualTo(NoticeErrorCode.INVALID_REMIND_AT);
    }

    @Test
    @DisplayName("현재보다 미래인 시각은 Bean Validation과 공지 도메인에서 모두 허용한다")
    void acceptFutureTimeTest() {
        LocalDateTime future = NOW.plusNanos(1);
        NoticeCreateRequest request = new NoticeCreateRequest("공지 제목", "공지 내용", List.of(future));
        Notice notice = createNotice();

        assertThat(validator.validate(request)).isEmpty();
        notice.addReminders(request.remindAts(), NOW);

        assertThat(notice.getNextRemindAt()).isEqualTo(future);
    }

    private Notice createNotice() {
        return Notice.create(
                mock(StudyMember.class),
                "공지 제목",
                "공지 내용"
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
