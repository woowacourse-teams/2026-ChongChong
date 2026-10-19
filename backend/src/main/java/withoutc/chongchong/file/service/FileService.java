package withoutc.chongchong.file.service;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import withoutc.chongchong.file.controller.dto.PresignedUrlCreateRequest;
import withoutc.chongchong.file.controller.dto.PresignedUrlCreateResponse;
import withoutc.chongchong.file.entity.FileMetadata;
import withoutc.chongchong.file.repository.FileMetaDataRepository;
import withoutc.chongchong.user.entity.User;
import withoutc.chongchong.user.repository.UserRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FileService {
    private final FileMetaDataRepository fileMetaDataRepository;
    private final UserRepository userRepository;
    private final FileUploadUrlGenerator fileUploadUrlGenerator;

    @Value("${aws.s3.upload-prefix}")
    private String uploadPrefix;

    @Transactional
    public PresignedUrlCreateResponse createPresignedUrl(Long userId, PresignedUrlCreateRequest request) {
        User user = userRepository.getByIdOrThrow(userId);

        String objectKey = uploadPrefix + UUID.randomUUID();
        FileMetadata fileMetadata = FileMetadata.create(user, request.originalFileName(), objectKey,
                request.contentType(), request.size());

        fileMetaDataRepository.save(fileMetadata);

        PresignedPutObjectRequest presignedRequest = fileUploadUrlGenerator.generatePresignedUrl(
                fileMetadata.getObjectKey(), fileMetadata.getContentType());

        return PresignedUrlCreateResponse.from(fileMetadata.getId(), presignedRequest);
    }
}
