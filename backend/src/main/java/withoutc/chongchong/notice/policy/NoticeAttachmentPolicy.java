package withoutc.chongchong.notice.policy;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import withoutc.chongchong.file.entity.FileMetadata;
import withoutc.chongchong.notice.exception.NoticeErrorCode;
import withoutc.chongchong.notice.exception.NoticeException;

@Component
public class NoticeAttachmentPolicy {

    private final int maxCount;
    private final long maxSizeBytes;
    private final Set<String> allowedContentTypes;

    public NoticeAttachmentPolicy(
            @Value("${notice.attachments.max-count}") int maxCount,
            @Value("${notice.attachments.max-size-bytes}") long maxSizeBytes,
            @Value("${notice.attachments.allowed-content-types}") List<String> allowedContentTypes
    ) {
        if (maxCount < 1 || maxSizeBytes < 1 || allowedContentTypes.isEmpty()) {
            throw new IllegalArgumentException("공지 첨부 정책 설정이 올바르지 않습니다.");
        }
        this.maxCount = maxCount;
        this.maxSizeBytes = maxSizeBytes;
        this.allowedContentTypes = Set.copyOf(allowedContentTypes);
    }

    public void validateIds(List<Long> fileIds) {
        if (fileIds.size() > maxCount) {
            throw new NoticeException(NoticeErrorCode.ATTACHMENT_LIMIT_EXCEEDED);
        }
        if (fileIds.stream().anyMatch(id -> id == null || id <= 0)
                || new HashSet<>(fileIds).size() != fileIds.size()) {
            throw new NoticeException(NoticeErrorCode.INVALID_ATTACHMENT_IDS);
        }
    }

    public void validateImage(FileMetadata file) {
        if (!allowedContentTypes.contains(file.getContentType())) {
            throw new NoticeException(NoticeErrorCode.INVALID_NOTICE_IMAGE_TYPE);
        }
        if (file.getSize() <= 0 || file.getSize() > maxSizeBytes) {
            throw new NoticeException(NoticeErrorCode.INVALID_NOTICE_IMAGE_SIZE);
        }
    }
}
