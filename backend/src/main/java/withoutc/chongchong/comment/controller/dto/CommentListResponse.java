package withoutc.chongchong.comment.controller.dto;

import java.util.List;

public record CommentListResponse(List<CommentResponse> comments, long totalCount, Long nextCursor, boolean hasNext) {

    public CommentListResponse {
        comments = List.copyOf(comments);
    }
}
