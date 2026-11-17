package withoutc.chongchong.file.service.cleanup;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import software.amazon.awssdk.services.s3.model.S3Error;
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
        Map<String, String> failures = new HashMap<>();
        List<ObjectIdentifier> identifiers = objectKeys.stream()
                .filter(key -> {
                    if (key.startsWith(uploadPrefix)) {
                        return true;
                    }
                    failures.put(key, OUTSIDE_UPLOAD_PREFIX);
                    return false;
                })
                .map(key -> ObjectIdentifier.builder().key(key).build())
                .toList();
        if (identifiers.isEmpty()) {
            return new DeleteResult(Set.of(), failures);
        }

        DeleteObjectsResponse response;
        try {
            response = s3Client.deleteObjects(DeleteObjectsRequest.builder()
                    .bucket(bucket)
                    .delete(Delete.builder().objects(identifiers).quiet(false).build())
                    .build());
        } catch (S3Exception exception) {
            String code = exception.awsErrorDetails() == null ? CLIENT_ERROR : exception.awsErrorDetails().errorCode();
            identifiers.forEach(identifier -> failures.put(identifier.key(), code));
            return new DeleteResult(Set.of(), failures);
        } catch (SdkException exception) {
            identifiers.forEach(identifier -> failures.put(identifier.key(), CLIENT_ERROR));
            return new DeleteResult(Set.of(), failures);
        }

        Set<String> deleted = new HashSet<>();
        for (DeletedObject object : response.deleted()) {
            deleted.add(object.key());
        }
        for (S3Error error : response.errors()) {
            deleted.remove(error.key());
            failures.put(error.key(), error.code());
        }
        identifiers.stream()
                .map(ObjectIdentifier::key)
                .filter(key -> !deleted.contains(key) && !failures.containsKey(key))
                .forEach(key -> failures.put(key, MISSING_RESULT));
        return new DeleteResult(deleted, failures);
    }

    public record DeleteResult(Set<String> deletedKeys, Map<String, String> failures) {
    }
}
