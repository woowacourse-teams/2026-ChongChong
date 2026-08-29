package withoutc.chongchong.global.pagination;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CursorPageRequestTest {

    @Test
    @DisplayName("커서는 생략하거나 양수로 지정할 수 있다")
    void allowNullOrPositiveCursorTest() {
        assertThat(CursorPageRequest.of(null, 10).cursor()).isNull();
        assertThat(CursorPageRequest.of(1L, 10).cursor()).isEqualTo(1L);
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1})
    @DisplayName("커서가 양수가 아니면 예외가 발생한다")
    void rejectNonPositiveCursorTest(long cursor) {
        assertThatThrownBy(() -> CursorPageRequest.of(cursor, 10))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("커서는 양수여야 합니다.");
    }

    @Test
    @DisplayName("최대 페이지 크기에서 다음 페이지 확인용 조회 크기를 계산한다")
    void calculateFetchSizeAtMaximumTest() {
        CursorPageRequest pageRequest = CursorPageRequest.of(null, CursorPageRequest.MAX_SIZE);

        assertThat(pageRequest.size()).isEqualTo(100);
        assertThat(pageRequest.fetchSize()).isEqualTo(101);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, 101, Integer.MAX_VALUE})
    @DisplayName("페이지 크기가 허용 범위를 벗어나면 예외가 발생한다")
    void rejectOutOfRangePageSizeTest(int size) {
        assertThatThrownBy(() -> CursorPageRequest.of(null, size))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("페이지 크기는 1 이상 100 이하여야 합니다.");
    }
}
