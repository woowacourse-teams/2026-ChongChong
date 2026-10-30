package withoutc.chongchong.comment.controller.dto;

import java.util.List;

public record CommentListResponse(List<CommentResponse> comments, Long nextCursor, boolean hasNext) {

    public CommentListResponse {
        comments = List.copyOf(comments);
    }
}
