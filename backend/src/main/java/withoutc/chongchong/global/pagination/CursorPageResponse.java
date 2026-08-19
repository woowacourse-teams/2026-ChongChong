package withoutc.chongchong.global.pagination;

import java.util.List;
import java.util.function.Function;

public record CursorPageResponse<T>(
        List<T> content,
        Long nextCursor,
        boolean hasNext
) {
    public static <T> CursorPageResponse<T> of(List<T> results, int size, Function<T, Long> cursorExtractor) {

        validateSize(size);
        boolean hasNext = results.size() > size;

        int contentSize = Math.min(size, results.size());
        List<T> content = List.copyOf(results.subList(0, contentSize));

        if (!hasNext) {
            return new CursorPageResponse<>(content, null, false);
        }

        T lastContent = content.getLast();
        Long nextCursor = cursorExtractor.apply(lastContent);

        return new CursorPageResponse<>(content, nextCursor, true);
    }

    private static void validateSize(int size) {
        if (size < 1) {
            throw new IllegalArgumentException("페이지 크기는 1 이상이어야 합니다.");
        }
    }
}
