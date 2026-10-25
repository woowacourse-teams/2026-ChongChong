package withoutc.chongchong.assignment.policy;

import jakarta.annotation.PostConstruct;
import java.util.HashSet;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import withoutc.chongchong.assignment.exception.AssignmentErrorCode;
import withoutc.chongchong.assignment.exception.AssignmentException;
import withoutc.chongchong.file.entity.FileMetadata;

@Component
public class AssignmentAttachmentPolicy {

    @Value("${assignment.attachments.max-count:10}")
    private int maxCount;

    @Value("${file.upload.max-size-bytes:5242880}")
    private long maxSizeBytes;

    @Value("${assignment.attachments.allowed-content-types:image/jpeg,image/png,image/webp,application/pdf,text/plain,application/zip,application/vnd.openxmlformats-officedocument.wordprocessingml.document,application/vnd.openxmlformats-officedocument.presentationml.presentation}")
    private List<String> allowedContentTypes;

    public void validateIds(List<Long> fileIds) {
        if (fileIds.size() > maxCount) {
            throw new AssignmentException(AssignmentErrorCode.ATTACHMENT_LIMIT_EXCEEDED);
        }
        if (fileIds.stream().anyMatch(id -> id == null || id <= 0)
                || new HashSet<>(fileIds).size() != fileIds.size()) {
            throw new AssignmentException(AssignmentErrorCode.INVALID_ATTACHMENT_IDS);
        }
    }

    public void validateFile(FileMetadata file) {
        if (!allowedContentTypes.contains(file.getContentType())) {
            throw new AssignmentException(AssignmentErrorCode.INVALID_ATTACHMENT_TYPE);
        }
        if (file.getSize() > maxSizeBytes) {
            throw new AssignmentException(AssignmentErrorCode.INVALID_ATTACHMENT_SIZE);
        }
    }

    public void validateSubmission(String content, String link, List<FileMetadata> files) {
        if ((content == null || content.isBlank()) && (link == null || link.isBlank()) && files.isEmpty()) {
            throw new AssignmentException(AssignmentErrorCode.EMPTY_SUBMISSION);
        }
    }

    @PostConstruct
    private void validateConfiguration() {
        if (maxCount < 1 || maxSizeBytes < 0 || allowedContentTypes.isEmpty()) {
            throw new IllegalArgumentException("과제 첨부 정책 설정이 올바르지 않습니다.");
        }
    }
}
