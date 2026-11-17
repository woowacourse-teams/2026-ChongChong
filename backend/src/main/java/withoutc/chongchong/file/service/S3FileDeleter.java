package withoutc.chongchong.file.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectsResponse;
import software.amazon.awssdk.services.s3.model.DeletedObject;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Component
@RequiredArgsConstructor
public class S3FileDeleter {

    static final String OUTSIDE_UPLOAD_PREFIX = "OutsideUploadPrefix";
    static final String MISSING_RESULT = "MissingResult";
    static final String CLIENT_ERROR = "ClientError";

    private static final int MAX_KEYS_PER_REQUEST = 1000;

    private final S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Value("${aws.s3.upload-prefix}")
    private String uploadPrefix;

    public DeleteResult delete(List<String> objectKeys) {
        if (objectKeys.size() > MAX_KEYS_PER_REQUEST) {
            throw new IllegalArgumentException("한 번에 삭제할 수 있는 객체는 1000개 이하입니다.");
        }
        Map<Boolean, List<String>> byPrefix = objectKeys.stream()
                .collect(Collectors.partitioningBy(key -> key.startsWith(uploadPrefix)));
        Map<String, String> failures = new HashMap<>();
        byPrefix.get(false).forEach(key -> failures.put(key, OUTSIDE_UPLOAD_PREFIX));

        List<String> deletableKeys = byPrefix.get(true);
        if (deletableKeys.isEmpty()) {
            return new DeleteResult(Set.of(), failures);
        }
        return requestDelete(deletableKeys, failures);
    }

    private DeleteResult requestDelete(List<String> keys, Map<String, String> failures) {
        try {
            DeleteObjectsResponse response = s3Client.deleteObjects(deleteRequest(keys));
            return toResult(keys, response, failures);
        } catch (SdkException exception) {
            String error = errorCode(exception);
            keys.forEach(key -> failures.put(key, error));
            return new DeleteResult(Set.of(), failures);
        }
    }

    private DeleteObjectsRequest deleteRequest(List<String> keys) {
        List<ObjectIdentifier> identifiers = keys.stream()
                .map(key -> ObjectIdentifier.builder().key(key).build())
                .toList();
        return DeleteObjectsRequest.builder()
                .bucket(bucket)
                .delete(Delete.builder().objects(identifiers).quiet(false).build())
                .build();
    }

    private DeleteResult toResult(List<String> keys, DeleteObjectsResponse response, Map<String, String> failures) {
        response.errors().forEach(error -> failures.put(error.key(), error.code()));
        Set<String> deleted = response.deleted().stream()
                .map(DeletedObject::key)
                .filter(key -> !failures.containsKey(key))
                .collect(Collectors.toSet());
        keys.stream()
                .filter(key -> !deleted.contains(key))
                .forEach(key -> failures.putIfAbsent(key, MISSING_RESULT));
        return new DeleteResult(deleted, failures);
    }

    private String errorCode(SdkException exception) {
        if (exception instanceof S3Exception s3Exception && s3Exception.awsErrorDetails() != null) {
            return s3Exception.awsErrorDetails().errorCode();
        }
        return CLIENT_ERROR;
    }

    public record DeleteResult(Set<String> deletedKeys, Map<String, String> failures) {

        public boolean isDeleted(String objectKey) {
            return deletedKeys.contains(objectKey);
        }

        public String errorOf(String objectKey) {
            return failures.getOrDefault(objectKey, MISSING_RESULT);
        }
    }
}
