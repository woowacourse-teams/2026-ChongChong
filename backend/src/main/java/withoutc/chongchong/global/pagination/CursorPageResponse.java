package withoutc.chongchong.global.pagination;

import java.util.List;
import java.util.function.Function;

public record CursorPageResponse<T>(
        List<T> content,
        Long nextCursor,
        boolean hasNext
) {
    public static <T> CursorPageResponse<T> of(
            List<T> results,
            CursorPageRequest pageRequest,
            Function<T, Long> cursorExtractor
    ) {
        int size = pageRequest.size();

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
}
