package withoutc.chongchong.global.pagination;

public record CursorPageRequest(
        Long cursor,
        int size
) {
    public static final int MAX_SIZE = 100;

    public CursorPageRequest {
        validateCursor(cursor);
        validateSize(size);
    }

    public static CursorPageRequest of(Long cursor, int size) {
        return new CursorPageRequest(cursor, size);
    }

    public int fetchSize() {
        return Math.addExact(size, 1);
    }

    private static void validateCursor(Long cursor) {
        if (cursor != null && cursor < 1) {
            throw new IllegalArgumentException("커서는 양수여야 합니다.");
        }
    }

    private static void validateSize(int size) {
        if (size < 1 || size > MAX_SIZE) {
            throw new IllegalArgumentException("페이지 크기는 1 이상 100 이하여야 합니다.");
        }
    }
}
