package withoutc.chongchong.notice.entity;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import withoutc.chongchong.file.entity.FileMetadata;

class NoticeAttachmentTest {

    @ParameterizedTest
    @ValueSource(ints = {-1, Integer.MIN_VALUE})
    @DisplayName("음수 표시 순서로 공지 첨부를 생성할 수 없다")
    void rejectNegativeSortOrder(int sortOrder) {
        Notice notice = mock(Notice.class);
        FileMetadata file = mock(FileMetadata.class);

        assertThatThrownBy(() -> NoticeAttachment.create(notice, file, sortOrder))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("첨부 표시 순서는 0 이상이어야 합니다.");
    }
}
