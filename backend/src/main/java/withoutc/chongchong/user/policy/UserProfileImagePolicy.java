package withoutc.chongchong.user.policy;

import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import withoutc.chongchong.file.entity.FileMetadata;
import withoutc.chongchong.user.exception.UserErrorCode;
import withoutc.chongchong.user.exception.UserException;

@Component
public class UserProfileImagePolicy {

    private final long maxSizeBytes;
    private final Set<String> allowedContentTypes;

    public UserProfileImagePolicy(
            @Value("${user.profile-image.max-size-bytes}") long maxSizeBytes,
            @Value("${user.profile-image.allowed-content-types}") List<String> allowedContentTypes
    ) {
        if (maxSizeBytes < 1 || allowedContentTypes.isEmpty()) {
            throw new IllegalArgumentException("사용자 프로필 이미지 정책 설정이 올바르지 않습니다.");
        }
        this.maxSizeBytes = maxSizeBytes;
        this.allowedContentTypes = Set.copyOf(allowedContentTypes);
    }

    public void validateId(Long fileId) {
        if (fileId == null || fileId <= 0) {
            throw new UserException(UserErrorCode.INVALID_USER_PROFILE_IMAGE_ID);
        }
    }

    public void validateImage(FileMetadata file) {
        if (!allowedContentTypes.contains(file.getContentType())) {
            throw new UserException(UserErrorCode.INVALID_USER_PROFILE_IMAGE_TYPE);
        }
        if (file.getSize() <= 0 || file.getSize() > maxSizeBytes) {
            throw new UserException(UserErrorCode.INVALID_USER_PROFILE_IMAGE_SIZE);
        }
    }
}
