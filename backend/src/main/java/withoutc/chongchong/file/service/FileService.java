package withoutc.chongchong.file.service;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import withoutc.chongchong.file.controller.dto.PresignedUrlCreateRequest;
import withoutc.chongchong.file.controller.dto.PresignedUrlCreateResponse;
import withoutc.chongchong.file.entity.FileMetadata;
import withoutc.chongchong.file.exception.FileErrorCode;
import withoutc.chongchong.file.exception.FileException;
import withoutc.chongchong.file.policy.FilePolicy;
import withoutc.chongchong.file.repository.FileMetaDataRepository;
import withoutc.chongchong.file.repository.projection.FileUploadSnapshotProjection;
import withoutc.chongchong.user.entity.User;
import withoutc.chongchong.user.repository.UserRepository;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class FileService {

    private final FileMetaDataRepository fileMetaDataRepository;
    private final UserRepository userRepository;
    private final FileUrlGenerator fileUrlGenerator;
    private final S3FileMetadataReader s3FileMetadataReader;
    private final FileUploadTransactionService fileUploadTransactionService;

    private final FilePolicy filePolicy;

    @Value("${aws.s3.upload-prefix}")
    private String uploadPrefix;

    @Transactional
    public PresignedUrlCreateResponse createPresignedUrl(Long userId, PresignedUrlCreateRequest request) {
        User user = userRepository.getByIdOrThrow(userId);
        filePolicy.validateSize(request.size());

        String purposePrefix = "";
        if (request.uploadPurpose() != null) {
            purposePrefix = request.uploadPurpose().getPrefix();
        }
        String objectKey = uploadPrefix + purposePrefix + UUID.randomUUID();
        FileMetadata fileMetadata = FileMetadata.create(user, request.originalFileName(), objectKey,
                request.contentType(), request.size());

        fileMetaDataRepository.save(fileMetadata);

        PresignedPutObjectRequest presignedRequest = fileUrlGenerator.generateUploadUrl(
                fileMetadata.getObjectKey(), fileMetadata.getContentType());

        return PresignedUrlCreateResponse.from(fileMetadata.getId(), presignedRequest);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void completeUpload(Long userId, Long fileId) {
        FileUploadSnapshotProjection file = fileUploadTransactionService.getOwnedFile(userId, fileId);
        if (filePolicy.isCompleted(file.status())) {
            return;
        }

        long actualSize = s3FileMetadataReader.findSize(file.objectKey())
                .orElseThrow(() -> new FileException(FileErrorCode.FILE_UPLOAD_NOT_FOUND));
        filePolicy.validateSize(actualSize);
        filePolicy.validateActualSize(file.size(), actualSize);

        try {
            fileUploadTransactionService.complete(userId, fileId, actualSize);
        } catch (PessimisticLockingFailureException e) {
            throw new FileException(FileErrorCode.FILE_UPLOAD_BUSY, e);
        }
    }
}
