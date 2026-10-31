package withoutc.chongchong.study.policy;

import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import withoutc.chongchong.file.entity.FileMetadata;
import withoutc.chongchong.study.exception.StudyErrorCode;
import withoutc.chongchong.study.exception.StudyException;

@Component
public class StudyImagePolicy {

    private final long maxSizeBytes;
    private final Set<String> allowedContentTypes;

    public StudyImagePolicy(
            @Value("${study.image.max-size-bytes}") long maxSizeBytes,
            @Value("${study.image.allowed-content-types}") List<String> allowedContentTypes
    ) {
        if (maxSizeBytes < 1 || allowedContentTypes.isEmpty()) {
            throw new IllegalArgumentException("스터디 이미지 첨부 정책 설정이 올바르지 않습니다.");
        }
        this.maxSizeBytes = maxSizeBytes;
        this.allowedContentTypes = Set.copyOf(allowedContentTypes);
    }

    public void validateId(Long fileId) {
        if (fileId == null || fileId <= 0) {
            throw new StudyException(StudyErrorCode.INVALID_IMAGE_ID);
        }
    }

    public void validateImage(FileMetadata file) {
        if (!allowedContentTypes.contains(file.getContentType())) {
            throw new StudyException(StudyErrorCode.INVALID_IMAGE_TYPE);
        }
        if (file.getSize() <= 0 || file.getSize() > maxSizeBytes) {
            throw new StudyException(StudyErrorCode.INVALID_IMAGE_SIZE);
        }
    }
}
