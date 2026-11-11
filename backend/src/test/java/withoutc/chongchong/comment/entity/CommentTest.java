package withoutc.chongchong.comment.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertTimeout;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import withoutc.chongchong.comment.exception.CommentErrorCode;
import withoutc.chongchong.comment.exception.CommentException;
import withoutc.chongchong.notification.entity.ResourceType;
import withoutc.chongchong.study.entity.StudyMember;
import withoutc.chongchong.user.entity.User;

class CommentTest {

    private static final int MAX_CONTENT_LENGTH = 1000;
    private static final int MAX_TWO_UNIT_EMOJI_COUNT = 500;

    @Test
    @DisplayName("trim으로 앞뒤 공백을 제거하고 본문 안의 공백과 줄바꿈은 유지한다")
    void normalizeContent() {
        Comment comment = comment("\t \n 첫  줄\n둘째\t줄 \r\n");

        assertThat(comment.getContent()).isEqualTo("첫  줄\n둘째\t줄");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\n\t", "\u2003", "\u001c\u001f"})
    @DisplayName("trim 후 isBlank인 댓글을 거부한다")
    void rejectBlankContent(String content) {
        assertThatThrownBy(() -> comment(content))
                .isInstanceOf(CommentException.class)
                .extracting(exception -> ((CommentException) exception).getErrorCode())
                .isEqualTo(CommentErrorCode.INVALID_CONTENT);
    }

    @Test
    @DisplayName("공지·과제처럼 String.length 기준 1000자까지 허용하고 초과하면 거부한다")
    void validateNormalizedStringLength() {
        String content = "가".repeat(MAX_CONTENT_LENGTH);
        String emojiContent = "😀".repeat(MAX_TWO_UNIT_EMOJI_COUNT);
        assertThat(comment(" ".repeat(MAX_CONTENT_LENGTH) + content + "\n").getContent()).isEqualTo(content);
        assertThat(comment(emojiContent).getContent()).isEqualTo(emojiContent);
        assertThat(comment("가").getContent()).isEqualTo("가");
        assertThatThrownBy(() -> comment(content + "가"))
                .isInstanceOf(CommentException.class);
        assertThatThrownBy(() -> comment(emojiContent + "가"))
                .isInstanceOf(CommentException.class);
    }

    @Test
    @DisplayName("실제 본문 변경만 수정 시각을 기록하며 같은 내용은 수정 표시를 만들지 않는다")
    void recordOnlyActualEdit() {
        Comment comment = comment("원문");
        LocalDateTime now = LocalDateTime.of(2026, 10, 30, 12, 0, 0, 123_456_789);

        comment.update(" 원문 ", now);
        assertThat(comment.getEditedAt()).isNull();
        comment.update("수정", now);
        assertThat(comment.getContent()).isEqualTo("수정");
        assertThat(comment.getEditedAt()).isEqualTo(now.truncatedTo(ChronoUnit.MICROS));
        comment.update(" 수정 ", now.plusDays(1));
        assertThat(comment.getEditedAt()).isEqualTo(now.truncatedTo(ChronoUnit.MICROS));
        assertThatThrownBy(() -> comment.update(" ", now.plusDays(1))).isInstanceOf(CommentException.class);
        assertThat(comment.getContent()).isEqualTo("수정");
    }

    @Test
    @DisplayName("긴 내부 공백은 빠르게 거부하고 긴 앞뒤 공백은 정규화 후 허용한다")
    void normalizeLongWhitespace() {
        Comment comment = comment("기존 본문");
        String invalid = "a" + " ".repeat(80_000) + "b";
        String valid = " \n".repeat(80_000) + "새 본문" + "\t ".repeat(80_000);
        LocalDateTime now = LocalDateTime.of(2026, 10, 30, 12, 0);

        assertTimeout(Duration.ofSeconds(1), () -> {
            assertThatThrownBy(() -> comment.update(invalid, now))
                    .isInstanceOf(CommentException.class)
                    .extracting(exception -> ((CommentException) exception).getErrorCode())
                    .isEqualTo(CommentErrorCode.INVALID_CONTENT);
            assertThat(comment.getContent()).isEqualTo("기존 본문");
            assertThat(comment.getEditedAt()).isNull();
            comment.update(valid, now);
            assertThat(comment.getContent()).isEqualTo("새 본문");
        });
    }

    @Test
    @DisplayName("trim 대상이 아닌 유니코드 공백은 유지하고 낮은 코드 값의 앞뒤 제어 문자는 제거한다")
    void followTrimWhitespaceRules() {
        assertThat(comment("\u00a0본문\u2003").getContent()).isEqualTo("\u00a0본문\u2003");
        assertThat(comment("\u00a0").getContent()).isEqualTo("\u00a0");
        assertThat(comment("\u001c본문\u001f").getContent()).isEqualTo("본문");
    }

    private Comment comment(String content) {
        StudyMember author = mock(StudyMember.class);
        when(author.getUser()).thenReturn(mock(User.class));
        return Comment.create(ResourceType.NOTICE, 1L, author, content);
    }
}
