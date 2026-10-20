package withoutc.chongchong.file.service;

import org.springframework.stereotype.Component;
import withoutc.chongchong.file.entity.FileMetadata;
import withoutc.chongchong.file.exception.FileErrorCode;
import withoutc.chongchong.file.exception.FileException;

@Component
public class UnavailableUploadedFileVerifier implements UploadedFileVerifier {

    @Override
    public void verify(FileMetadata file) {
        throw new FileException(FileErrorCode.FILE_VERIFICATION_UNAVAILABLE);
    }
}
