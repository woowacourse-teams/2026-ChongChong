package withoutc.chongchong.user.service;

import jakarta.persistence.EntityManager;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import withoutc.chongchong.file.entity.FileMetadata;
import withoutc.chongchong.file.exception.FileErrorCode;
import withoutc.chongchong.file.exception.FileException;
import withoutc.chongchong.file.repository.FileMetaDataRepository;
import withoutc.chongchong.user.entity.User;
import withoutc.chongchong.user.policy.UserProfileImagePolicy;

@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class UserProfileImageService {

    private final FileMetaDataRepository fileRepository;
    private final UserProfileImagePolicy imagePolicy;
    private final EntityManager entityManager;

    // 호출자는 사용자 쓰기 잠금을 먼저 획득하고 같은 트랜잭션에서 검증 결과를 연결한다.
    public FileMetadata prepareForUpdate(User user, Long fileId) {
        imagePolicy.validateId(fileId);
        Long currentFileId = currentFileId(user);
        FileMetadata file = findFilesForUpdate(currentFileId, fileId).get(fileId);
        validateOwnedCompletedFile(user.getId(), file);
        if (fileId.equals(currentFileId)) {
            return file;
        }
        validateNewFile(file);
        return file;
    }

    // 호출자는 사용자 쓰기 잠금을 유지한 채 홈 파일 연결과 외부 URL을 함께 해제한다.
    public void prepareForReset(User user) {
        Long fileId = currentFileId(user);
        if (fileId == null) {
            return;
        }
        findFileForUpdate(fileId);
    }

    private Long currentFileId(User user) {
        return user.getProfileImageFile() == null ? null : user.getProfileImageFile().getId();
    }

    private Map<Long, FileMetadata> findFilesForUpdate(Long currentFileId, Long newFileId) {
        return Stream.of(currentFileId, newFileId)
                .filter(Objects::nonNull)
                .distinct()
                .sorted()
                .map(this::findFileForUpdate)
                .collect(Collectors.toMap(FileMetadata::getId, Function.identity()));
    }

    private FileMetadata findFileForUpdate(Long fileId) {
        try {
            FileMetadata file = fileRepository.getByIdForUpdateOrThrow(fileId);
            entityManager.refresh(file);
            return file;
        } catch (PessimisticLockingFailureException exception) {
            throw new FileException(FileErrorCode.FILE_UPLOAD_BUSY, exception);
        }
    }

    private void validateOwnedCompletedFile(Long userId, FileMetadata file) {
        if (!file.isOwnedBy(userId)) {
            throw new FileException(FileErrorCode.FILE_NOT_OWNED);
        }
        if (!file.isCompleted()) {
            throw new FileException(FileErrorCode.FILE_NOT_ATTACHABLE);
        }
    }

    private void validateNewFile(FileMetadata file) {
        imagePolicy.validateImage(file);
        if (fileRepository.existsAnyUsageByFileId(file.getId())) {
            throw new FileException(FileErrorCode.FILE_NOT_ATTACHABLE);
        }
    }
}
