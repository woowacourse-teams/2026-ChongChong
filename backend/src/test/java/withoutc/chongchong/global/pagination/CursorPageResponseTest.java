package withoutc.chongchong.global.pagination;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CursorPageResponseTest {

    @Test
    @DisplayName("조회 결과가 요청 크기보다 하나 많으면 요청 크기만큼 반환하고 다음 커서를 만든다")
    void createNextPageCursorWhenResultsContainExtraElement() {
        List<TestItem> results = List.of(
                new TestItem(10L),
                new TestItem(9L),
                new TestItem(8L)
        );

        CursorPageResponse<TestItem> response = CursorPageResponse.of(results, pageRequest(), TestItem::id);

        assertThat(response.content()).containsExactly(results.get(0), results.get(1));
        assertThat(response.nextCursor()).isEqualTo(9L);
        assertThat(response.hasNext()).isTrue();
    }

    @Test
    @DisplayName("조회 결과가 요청 크기와 같으면 다음 페이지가 없다고 응답한다")
    void createLastPageWhenResultSizeEqualsRequestedSize() {
        List<TestItem> results = List.of(new TestItem(10L), new TestItem(9L));

        CursorPageResponse<TestItem> response = CursorPageResponse.of(results, pageRequest(), TestItem::id);

        assertThat(response.content()).containsExactlyElementsOf(results);
        assertThat(response.nextCursor()).isNull();
        assertThat(response.hasNext()).isFalse();
    }

    @Test
    @DisplayName("조회 결과가 요청 크기보다 적으면 모든 결과를 담은 마지막 페이지를 만든다")
    void createLastPageWhenResultSizeIsLessThanRequestedSize() {
        List<TestItem> results = List.of(new TestItem(10L));

        CursorPageResponse<TestItem> response = CursorPageResponse.of(results, pageRequest(), TestItem::id);

        assertThat(response.content()).containsExactlyElementsOf(results);
        assertThat(response.nextCursor()).isNull();
        assertThat(response.hasNext()).isFalse();
    }

    @Test
    @DisplayName("조회 결과가 비어 있으면 빈 마지막 페이지를 만든다")
    void createEmptyLastPage() {
        CursorPageResponse<TestItem> response = CursorPageResponse.of(List.of(), pageRequest(), TestItem::id);

        assertThat(response.content()).isEmpty();
        assertThat(response.nextCursor()).isNull();
        assertThat(response.hasNext()).isFalse();
    }

    @Test
    @DisplayName("페이지 내용은 원본 목록 변경의 영향을 받지 않는 불변 스냅샷이다")
    void copyContentAsImmutableSnapshot() {
        TestItem originalFirstItem = new TestItem(10L);
        List<TestItem> results = new ArrayList<>(List.of(originalFirstItem, new TestItem(9L)));
        CursorPageResponse<TestItem> response = CursorPageResponse.of(results, pageRequest(), TestItem::id);

        results.set(0, new TestItem(1L));

        assertThat(response.content()).first().isEqualTo(originalFirstItem);
        assertThatThrownBy(() -> response.content().add(new TestItem(8L)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    private CursorPageRequest pageRequest() {
        return CursorPageRequest.of(null, 2);
    }

    private record TestItem(Long id) {
    }
}
