package withoutc.chongchong.global.pagination;

public record CursorPageRequest(
        Long cursor,
        int size
) {
    public static final int MAX_SIZE = 100;
    private static final int MIN_SIZE = 1;
    private static final int NEXT_PAGE_LOOKAHEAD = 1;
    private static final long MIN_ID_CURSOR = 1;
    private static final String INVALID_CURSOR_MESSAGE = "커서는 양수여야 합니다.";
    private static final String INVALID_SIZE_MESSAGE = "페이지 크기는 1 이상 100 이하여야 합니다.";

    public CursorPageRequest {
        validateIdCursor(cursor);
        validateSize(size);
    }

    public static CursorPageRequest of(Long cursor, int size) {
        return new CursorPageRequest(cursor, size);
    }

    public int fetchSize() {
        return Math.addExact(size, NEXT_PAGE_LOOKAHEAD);
    }

    private static void validateIdCursor(Long cursor) {
        if (cursor != null && cursor < MIN_ID_CURSOR) {
            throw new IllegalArgumentException(INVALID_CURSOR_MESSAGE);
        }
    }

    private static void validateSize(int size) {
        if (size < MIN_SIZE || size > MAX_SIZE) {
            throw new IllegalArgumentException(INVALID_SIZE_MESSAGE);
        }
    }
}
