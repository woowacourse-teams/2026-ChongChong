package withoutc.chongchong.file.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import withoutc.chongchong.file.exception.FileErrorCode;
import withoutc.chongchong.file.exception.FileException;
import withoutc.chongchong.global.persistence.BaseEntity;
import withoutc.chongchong.user.entity.User;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "file_metadata")
public class FileMetadata extends BaseEntity {

    private static final int MAX_ORIGINAL_FILE_NAME_LENGTH = 255;
    private static final int MAX_OBJECT_KEY_LENGTH = 1024;
    private static final int MAX_CONTENT_TYPE_LENGTH = 255;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(name = "original_file_name", nullable = false)
    private String originalFileName;

    @Column(name = "object_key", nullable = false, unique = true, length = 1024)
    private String objectKey;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(nullable = false)
    private long size;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FileUploadStatus status;


    public static FileMetadata create(User owner, String originalFileName, String objectKey, String contentType,
                                      long size) {
        return new FileMetadata(owner, originalFileName, objectKey, contentType, size, FileUploadStatus.PENDING);
    }

    public boolean isOwnedBy(Long userId) {
        Long ownerId = owner.getId();
        return ownerId != null && ownerId.equals(userId);
    }

    public void completeUpload() {
        if (status != FileUploadStatus.PENDING) {
            throw new FileException(FileErrorCode.FILE_NOT_ATTACHABLE);
        }
        status = FileUploadStatus.COMPLETED;
    }

    public boolean isCompleted() {
        return status == FileUploadStatus.COMPLETED;
    }

    public void markForDeletion() {
        if (status != FileUploadStatus.COMPLETED) {
            throw new FileException(FileErrorCode.FILE_NOT_ATTACHABLE);
        }
        status = FileUploadStatus.DELETE_PENDING;
    }

    private FileMetadata(User owner, String originalFileName, String objectKey, String contentType, long size,
                         FileUploadStatus status) {
        validateOwner(owner);
        validateOriginalFileName(originalFileName);
        validateObjectKey(objectKey);
        validateContentType(contentType);
        validateSize(size);

        this.owner = owner;
        this.originalFileName = originalFileName;
        this.objectKey = objectKey;
        this.contentType = contentType;
        this.size = size;
        this.status = status;
    }

    private static void validateOwner(User owner) {
        if (owner == null) {
            throw new FileException(FileErrorCode.INVALID_FILE_OWNER);
        }
    }

    private static void validateOriginalFileName(String originalFileName) {
        if (originalFileName == null || originalFileName.isBlank()
                || originalFileName.length() > MAX_ORIGINAL_FILE_NAME_LENGTH) {
            throw new FileException(FileErrorCode.INVALID_ORIGINAL_FILE_NAME);
        }
    }

    private static void validateObjectKey(String objectKey) {
        if (objectKey == null || objectKey.isBlank() || objectKey.length() > MAX_OBJECT_KEY_LENGTH) {
            throw new FileException(FileErrorCode.INVALID_OBJECT_KEY);
        }
    }

    private static void validateContentType(String contentType) {
        if (contentType == null || contentType.isBlank() || contentType.length() > MAX_CONTENT_TYPE_LENGTH) {
            throw new FileException(FileErrorCode.INVALID_CONTENT_TYPE);
        }
    }

    private static void validateSize(long size) {
        if (size < 0) {
            throw new FileException(FileErrorCode.INVALID_FILE_SIZE);
        }
    }
}
