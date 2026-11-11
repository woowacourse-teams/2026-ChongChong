package withoutc.chongchong.comment.controller.dto;

import jakarta.validation.constraints.NotNull;

public record CommentWriteRequest(
        @NotNull(message = "댓글 내용은 필수 값입니다.") String content
) {
}
