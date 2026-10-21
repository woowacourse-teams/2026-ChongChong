package withoutc.chongchong.file.policy;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import withoutc.chongchong.file.entity.FileUploadStatus;
import withoutc.chongchong.file.exception.FileErrorCode;
import withoutc.chongchong.file.exception.FileException;

@Component
public class FilePolicy {

    @Value("${file.upload.max-size-bytes:5242880}")
    private long maxSizeBytes;

    @PostConstruct
    private void validateConfiguration() {
        if (maxSizeBytes < 0) {
            throw new IllegalArgumentException("파일 크기 제한은 0 이상이어야 합니다.");
        }
    }

    public void validateSize(long size) {
        if (size < 0 || size > maxSizeBytes) {
            throw new FileException(FileErrorCode.INVALID_FILE_SIZE);
        }
    }

    public void validateCompletion(Long userId, Long ownerId, FileUploadStatus status) {
        if (ownerId == null || !ownerId.equals(userId)) {
            throw new FileException(FileErrorCode.FILE_NOT_OWNED);
        }
        if (status != FileUploadStatus.PENDING && !isCompleted(status)) {
            throw new FileException(FileErrorCode.FILE_UPLOAD_NOT_PENDING);
        }
    }

    public void validateActualSize(long expectedSize, long actualSize) {
        if (expectedSize != actualSize) {
            throw new FileException(FileErrorCode.FILE_SIZE_MISMATCH);
        }
    }

    public boolean isCompleted(FileUploadStatus status) {
        return status == FileUploadStatus.COMPLETED;
    }
}
